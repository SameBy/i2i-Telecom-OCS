package com.i2i.telecom.service2;

import com.i2i.telecom.model.CustomerDB;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class ChargingListener {

    private final String BOT_TOKEN = "8942719783:AAEjiOmniAjDPqOg7dYZHrPrt38k1HCSEkI";
    private final String CHAT_ID = "8787589344";

    @KafkaListener(topics = "telecom-charging-logs", groupId = "telecom-ocs-group")
    public void processCallCharging(String rawEvent) {
        System.out.println("==================================================");
        System.out.println("[OCS ASYNCHRONOUS ENGINE] New Message Captured!");
        
        try {
            String msisdn = extractField(rawEvent, "msisdn");
            String status = extractField(rawEvent, "status");
            String reason = extractField(rawEvent, "rejectReason");
            String durationStr = extractField(rawEvent, "durationInMinutes");
            int duration = durationStr.isEmpty() ? 0 : Integer.parseInt(durationStr);

            System.out.println("[STREAM DATA] Sub: " + msisdn + " | Status: " + status + " | Reason: " + reason);

            if ("SUCCESS".equals(status)) {
                int oldBalance = CustomerDB.balances.getOrDefault(msisdn, 0);
                int newBalance = Math.max(0, oldBalance - duration);
                CustomerDB.balances.put(msisdn, newBalance);

                if (newBalance <= 5 && newBalance > 0) {
                    sendSms(msisdn, "Değerli müşterimiz, kalan paketiniz " + newBalance + " dakikanın altına düşmüştür. Ek paket almak için EK100 yazıp 2222'ye gönderebilirsiniz.");
                } else if (newBalance == 0) {
                    sendSms(msisdn, "Paketiniz tükenmiştir. Aramalarınız standart tarife üzerinden ücretlendirilecektir.");
                }
            } else if ("FAILED".equals(status)) {
                if ("INTERNATIONAL_BARRED".equals(reason)) {
                    sendSms(msisdn, "Bilgilendirme: Yurt dışı arama yetkiniz kapalı olduğu için aramanız gerçekleştirilemedi. Yurt dışı paketlerini incelemek ve hattınızı dünyaya açmak için hemen tıklayın: i2i.li/global");
                } else if ("INSUFFICIENT_BALANCE".equals(reason)) {
                    sendSms(msisdn, "Bilgilendirme: Yetersiz bakiye sebebiyle arama başarısız oldu. Hattınıza anında bakiye yüklemek ve %20 hediyeli paketleri kaçırmamak için tıklayın: i2i.li/tl-yukle");
                }
            }
        } catch (Exception e) {
            System.out.println("[PARSING ERROR] Failed to process incoming event: " + e.getMessage());
        }
        System.out.println("==================================================");
    }

    private String extractField(String json, String field) {
        try {
            int start = json.indexOf("\"" + field + "\"");
            if (start == -1) return "";
            start = json.indexOf(":", start);
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            String val = json.substring(start + 1, end).trim();
            return val.replaceAll("\"", "");
        } catch (Exception e) {
            return "";
        }
    }

    private void sendSms(String msisdn, String messageContent) {
        try {
            String formattedMessage = "📱 [i2i SMS Gateway]\nTo: " + msisdn + "\n\n" + messageContent;
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(formattedMessage, StandardCharsets.UTF_8);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("[SMS SENT SUCCESSFULLY] Notification dispatched via Telegram Gateway.");
        } catch (Exception e) {
            System.out.println("[SMS ERROR] Failed to send telegram notification: " + e.getMessage());
        }
    }
}