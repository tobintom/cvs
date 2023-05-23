package com.cvshealth.cvs.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Util {

    public static String getBase64URLEncodedString(String inputValue){
        String returnvalue = inputValue;
        try{
            // Convert byte array to Base64 string
            String base64String = Base64.getEncoder().withoutPadding().encodeToString(inputValue.getBytes());

            // URL encode the Base64 string
            String urlEncodedString = URLEncoder.encode(base64String,"UTF-8");

            // Convert to Base64URL
            String base64URLEncodedString = urlEncodedString.replace("+", "-")
                    .replace("/", "_")
                    .replace("=", "");
            returnvalue = base64URLEncodedString;
        }catch(Exception e){
            e.printStackTrace();
        }
        return returnvalue;
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
