package com.example.fraudulens.utils;

public class PhoneFormatUtil {
    private PhoneFormatUtil() {}

    public static String digitsOnly(String input) {
        if (input == null) return "";
        return input.replaceAll("[^0-9]", "");
    }

    public static String toLocal10(String input) {
        String digits = digitsOnly(input);
        if (digits.startsWith("63") && digits.length() >= 12) {
            digits = digits.substring(2);
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits;
    }

    public static String formatLocal(String input) {
        String local = toLocal10(input);
        if (local.length() != 10) return input != null ? input : "";
        return local.substring(0, 3) + " " + local.substring(3, 6) + " " + local.substring(6);
    }

    public static String toE164(String input) {
        String local = toLocal10(input);
        if (local.length() == 10) {
            return "+63" + local;
        }
        if (input != null && input.startsWith("+")) return input;
        return input != null ? input : "";
    }

    public static String toE164(String localInput, String countryCode) {
        String digits = digitsOnly(localInput);
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return toE164(localInput);
        }
        String cc = countryCode.trim();
        if (!cc.startsWith("+")) {
            cc = "+" + cc;
        }
        return cc + digits;
    }
}
