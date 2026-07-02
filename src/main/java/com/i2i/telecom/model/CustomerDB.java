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
        db.put("5551112233", new Customer("5551112233", 15, true));
        db.put("5554445566", new Customer("5554445566", 20, false));
        db.put("5557778899", new Customer("5557778899", 0, false));
    }
}
