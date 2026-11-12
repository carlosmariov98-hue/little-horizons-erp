package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.exception.ConfigurationException;

public class Main {

    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig();
            System.out.println("URL: " + config.getRequired("db.url"));
            System.out.println("User: " + config.getRequired("db.user"));
            System.out.println("Password length: " + config.getOptional("db.password", "").length());
        } catch (ConfigurationException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}