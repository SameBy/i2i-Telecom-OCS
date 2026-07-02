package com.i2i.telecom.service1;

import com.i2i.telecom.model.CustomerDB;
import com.i2i.telecom.model.CustomerDB.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class CallController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("service", "Service 1 - Auth Server");
        status.put("status", "UP");
        return status;
    }

    @PostMapping("/authorize-call")
    public Map<String, Object> authorizeCall(
            @RequestParam String msisdn,
            @RequestParam String destination,
            @RequestParam int duration,
            @RequestParam(defaultValue = "false") boolean isInternational) {

        Map<String, Object> response = new HashMap<>();
        Customer customer = CustomerDB.db.get(msisdn);

        if (customer == null) {
            response.put("status", "FAILED");
            response.put("message", "Subscriber not found.");
            return response;
        }

        boolean balanceDeficit = customer.remainingMinutes < duration || customer.remainingMinutes <= 0;
        boolean internationalBlocked = isInternational && !customer.isInternationalAllowed;

        if (balanceDeficit && internationalBlocked) {
            String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"FAILED_BOTH\"}", msisdn, duration);
            kafkaTemplate.send("telecom-charging-logs", kafkaPayload);

            response.put("status", "FAILED");
            response.put("message", "Call blocked: Insufficient balance AND international calling is disabled for this subscriber.");
            return response;
        }

        if (internationalBlocked) {
            String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"FAILED_INTERNATIONAL\"}", msisdn, duration);
            kafkaTemplate.send("telecom-charging-logs", kafkaPayload);

            response.put("status", "FAILED");
            response.put("message", "Call blocked: International calling is barred for this profile.");
            return response;
        }

        if (balanceDeficit) {
            String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"FAILED_REJECTED\"}", msisdn, duration);
            kafkaTemplate.send("telecom-charging-logs", kafkaPayload);

            response.put("status", "FAILED");
            response.put("message", "Call blocked: Insufficient balance. Current balance: " + customer.remainingMinutes + " mins.");
            return response;
        }

        customer.remainingMinutes -= duration;
        
        String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"SUCCESS\",\"initialMinutes\":%d,\"remainingMinutes\":%d}", 
                msisdn, duration, customer.initialMinutes, customer.remainingMinutes);
        kafkaTemplate.send("telecom-charging-logs", kafkaPayload);

        response.put("status", "SUCCESS");
        response.put("message", "Call authorized successfully. Remaining balance: " + customer.remainingMinutes + " mins. Event streamed to Kafka.");
        return response;
    }
}
