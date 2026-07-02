package com.i2i.telecom.service1;

import com.i2i.telecom.model.CallEvent;
import com.i2i.telecom.model.CustomerDB;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CallController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CallController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/")
    public Map<String, String> index() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Service 1 - Auth Server");
        return response;
    }

    @PostMapping("/authorize-call")
    public Map<String, String> authorizeCall(@RequestParam String msisdn,
                                             @RequestParam String destination,
                                             @RequestParam(defaultValue = "false") boolean isInternational,
                                             @RequestParam int duration) {
        Map<String, String> response = new HashMap<>();

        if (!CustomerDB.balances.containsKey(msisdn)) {
            CallEvent event = new CallEvent(msisdn, destination, isInternational, duration, "FAILED", "SUBSCRIBER_NOT_FOUND");
            kafkaTemplate.send("telecom-charging-logs", msisdn, event);
            response.put("status", "FAILED");
            response.put("message", "Subscriber not found.");
            return response;
        }

        int currentBalance = CustomerDB.balances.get(msisdn);
        boolean isIntAllowed = CustomerDB.internationalAllowed.get(msisdn);

        if (currentBalance <= 0) {
            CallEvent event = new CallEvent(msisdn, destination, isInternational, duration, "FAILED", "INSUFFICIENT_BALANCE");
            kafkaTemplate.send("telecom-charging-logs", msisdn, event);
            response.put("status", "FAILED");
            response.put("message", "Insufficient balance. Call blocked.");
            return response;
        }

        if (isInternational && !isIntAllowed) {
            CallEvent event = new CallEvent(msisdn, destination, isInternational, duration, "FAILED", "INTERNATIONAL_BARRED");
            kafkaTemplate.send("telecom-charging-logs", msisdn, event);
            response.put("status", "FAILED");
            response.put("message", "International calls are barred for this subscriber.");
            return response;
        }

        CallEvent event = new CallEvent(msisdn, destination, isInternational, duration, "SUCCESS", "NONE");
        kafkaTemplate.send("telecom-charging-logs", msisdn, event);
        response.put("status", "SUCCESS");
        response.put("message", "Call authorized successfully. Event streamed to Kafka.");
        return response;
    }
}
