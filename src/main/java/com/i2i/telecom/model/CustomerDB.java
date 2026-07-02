package com.i2i.telecom.model;

import java.util.concurrent.ConcurrentHashMap;

public class CustomerDB {
    public static class Customer {
        public String msisdn;
        public int initialMinutes;
        public int remainingMinutes;
        public boolean isInternationalAllowed;

        public Customer(String msisdn, int minutes, boolean isInternationalAllowed) {
            this.msisdn = msisdn;
            this.initialMinutes = minutes;
            this.remainingMinutes = minutes;
            this.isInternationalAllowed = isInternationalAllowed;
        }
    }

    public static ConcurrentHashMap<String, Customer> db = new ConcurrentHashMap<>();

    static {
        // 10 Farklı Senaryo İçin 10 Benzersiz Telefon Numarası
        db.put("5551110001", new Customer("5551110001", 50, true));  // 1. Standart Yurt İçi
        db.put("5551110002", new Customer("5551110002", 12, true));  // 2. Kritik Bakiye Uyarısı
        db.put("5551110003", new Customer("5551110003", 0, true));   // 3. Sıfır Bakiye Engeli
        db.put("5551110004", new Customer("5551110004", 30, true));  // 4. Yurt Dışı İzinli Başarı
        db.put("5551110005", new Customer("5551110005", 25, false)); // 5. Yurt Dışı Yasak Engeli
        db.put("5551110006", new Customer("5551110006", 0, false));  // 6. İKİLİ KRİZ (0 Bakiye + Yasak)
        db.put("5551110007", new Customer("5551110007", 3, true));   // 7. Akıllı Kısmi İzin (3dk var, 10dk iste)
        db.put("5551110008", new Customer("5551110008", 15, true));  // 8. Paket Tükenme Sınırı
        db.put("5551110010", new Customer("5551110010", 100, true)); // 10. Yüksek Süreli Kullanım
        // Not: 9. senaryo (Abone Bulunamadı) için DB'ye kayıt atmıyoruz.
    }
}
