package com.i2i.telecom.service2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ChargingListener {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String TELEGRAM_API = "https://api.telegram.org/bot8866456965:AAH5IDH6sFrXLkCvH5m2x94byMTkt2hiSyo/sendMessage";
    private final String CHAT_ID = "8787589344"; 

    // auto.offset.reset=latest ayarı ile geçmişteki test loglarını tamamen yok sayıyoruz
    @KafkaListener(
        topics = "telecom-charging-logs", 
        groupId = "telecom-ocs-group-prod-v5",
        properties = {"auto.offset.reset=latest"}
    )
    public void consumeCallEvent(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            
            if (json.isTextual()) {
                json = objectMapper.readTree(json.asText());
            }

            String msisdn = json.get("msisdn").asText();
            String status = json.get("status").asText();
            String smsText = "";

            if ("FAILED_BOTH".equals(status)) {
                smsText = "🚨 [i2i OCS Bilgilendirme]\nSayın Abonemiz (" + msisdn + "), hattınızda bakiye bulunmamaktadır VE yurt dışı arama yetkiniz kapalıdır. Lütfen bakiye yükleyiniz.";
            } 
            else if ("FAILED_INTERNATIONAL".equals(status)) {
                smsText = "🚫 [i2i OCS Bilgilendirme]\nSayın Abonemiz (" + msisdn + "), yurt dışı arama yetkiniz bulunmamaktadır.";
            } 
            else if ("FAILED_REJECTED".equals(status)) {
                smsText = "📉 [i2i OCS Bilgilendirme]\nSayın Abonemiz (" + msisdn + "), yetersiz bakiye nedeniyle arama gerçekleştirilemedi. Bakiyeniz 0 dakikadır.";
            } 
            else if ("SUCCESS".equals(status)) {
                int remaining = json.has("remainingMinutes") ? json.get("remainingMinutes").asInt() : 0;
                int actualDuration = json.has("actualDuration") ? json.get("actualDuration").asInt() : 0;
                int initial = json.has("initialMinutes") ? json.get("initialMinutes").asInt() : 100;
                
                double ratio = (initial > 0) ? (double) remaining / initial : 0;

                if (remaining == 0) {
                    smsText = "📉 [i2i OCS Bilgilendirme]\nSayın Abonemiz (" + msisdn + "), paketiniz görüşme sırasında tamamen tükenmiştir. Konuşmanız otomatik olarak " + actualDuration + ". dakikada sonlandırılmıştır.";
                } else if (ratio <= 0.20) {
                    smsText = "⚠️ [i2i OCS Bilgilendirme]\nSayın Abonemiz (" + msisdn + "), paketiniz bitmek üzere! Ekstra ücretlendirilmemek için hemen yeni bir paket satın alabilirsiniz.\nSatın Almak İçin: https://i2i.li/hizli-paket\nKalan Süre: " + remaining + " dk.";
                }
            }

            if (!smsText.isEmpty()) {
                sendTelegramSms(smsText);
            }

        } catch (Exception e) {
            System.err.println("Error processing consumer message: " + e.getMessage());
        }
    }

    private void sendTelegramSms(String text) {
        try {
            String jsonPayload = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\"}", CHAT_ID, text.replace("\n", "\\n"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TELEGRAM_API))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error sending payload to API: " + e.getMessage());
        }
    }
}
