package ru.syncra.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SignatureVerifier {

    private static final String SECRET_KEY = "LLSDaslwNJ#J!@K#JKeSjI@#I";

    public static boolean verify(String timestamp, String salt, String signature) throws Exception {
        String payload = timestamp + salt;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(payload.getBytes());
        String expectedSignature = Base64.getEncoder().encodeToString(hmacBytes);
        return expectedSignature.equals(signature);
    }

    public static boolean isTimestampValid(long timestamp) {
        long now = System.currentTimeMillis() / 1000;
        return Math.abs(now - timestamp) < 30;
    }

}
