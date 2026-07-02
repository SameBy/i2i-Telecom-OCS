package com.i2i.telecom.model;

import java.util.HashMap;
import java.util.Map;

public class CustomerDB {
    public static final Map<String, Integer> balances = new HashMap<>();
    public static final Map<String, Boolean> internationalAllowed = new HashMap<>();

    static {
        balances.put("5551112233", 15);
        internationalAllowed.put("5551112233", true);

        balances.put("5554445566", 5);
        internationalAllowed.put("5554445566", false);

        balances.put("5557778899", 0);
        internationalAllowed.put("5557778899", true);
    }
}