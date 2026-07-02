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

    @PostMapping("/reset-db")
    public Map<String, String> resetDb() {
        CustomerDB.db.values().forEach(c -> c.remainingMinutes = c.initialMinutes);
        Map<String, String> res = new HashMap<>();
        res.put("status", "SUCCESS");
        res.put("message", "All balances reset successfully.");
        return res;
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

        boolean balanceDeficit = customer.remainingMinutes <= 0;
        boolean partialAvailable = customer.remainingMinutes > 0 && customer.remainingMinutes < duration;
        boolean internationalBlocked = isInternational && !customer.isInternationalAllowed;

        // Kural A: Hem bakiye yetersiz/yok hem de yurt dışı kapalıysa (İkili Kriz UX)
        if ((balanceDeficit || partialAvailable) && internationalBlocked) {
            String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"FAILED_BOTH\"}", msisdn, duration);
            kafkaTemplate.send("telecom-charging-logs", kafkaPayload);
            response.put("status", "FAILED");
            response.put("message", "Call blocked: Insufficient balance AND international calling is disabled for this subscriber.");
            return response;
        }

        // Kural B: Sadece yurt dışı kapalıysa
        if (internationalBlocked) {
            String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"FAILED_INTERNATIONAL\"}", msisdn, duration);
            kafkaTemplate.send("telecom-charging-logs", kafkaPayload);
            response.put("status", "FAILED");
            response.put("message", "Call blocked: International calling is barred for this profile.");
            return response;
        }

        // Kural C: Tamamen bakiye sıfır ise
        if (balanceDeficit) {
            String kafkaPayload = String.format("{\"msisdn\":\"%s\",\"duration\":%d,\"status\":\"FAILED_REJECTED\"}", msisdn, duration);
            kafkaTemplate.send("telecom-charging-logs", kafkaPayload);
            response.put("status", "FAILED");
            response.put("message", "Call blocked: Insufficient balance. Current balance: 0 mins.");
            return response;
        }

        // Kural D: Akıllı Kısmi Kullanım (1 dk varsa 5 dk arayınca 1 dk konuştur, sonra kapat)
        int actualDuration = duration;
        if (partialAvailable) {
            actualDuration = customer.remainingMinutes;
        }

        customer.remainingMinutes -= actualDuration;

        String kafkaPayload = String.format(
            "{\"msisdn\":\"%s\",\"actualDuration\":%d,\"status\":\"SUCCESS\",\"initialMinutes\":%d,\"remainingMinutes\":%d}",
            msisdn, actualDuration, customer.initialMinutes, customer.remainingMinutes
        );
        kafkaTemplate.send("telecom-charging-logs", kafkaPayload);

        response.put("status", "SUCCESS");
        response.put("message", "Call authorized for " + actualDuration + " mins (Requested: " + duration + " mins). Remaining: " + customer.remainingMinutes + " mins.");
        return response;
    }
}
