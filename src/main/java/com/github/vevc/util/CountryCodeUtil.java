package com.github.vevc.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Country code utility for IP geolocation
 * Automatically detects server country code from IP
 * @author zv
 */
public final class CountryCodeUtil {

    private static final String IP_API_URL = "http://ip-api.com/json/?fields=countryCode";
    private static final String IP_SB_URL = "https://api.ip.sb/geoip/%s";
    private static final int TIMEOUT_MS = 5000;

    /**
     * Get country code from server's public IP
     * Uses multiple APIs with fallback
     */
    public static String getCountryCode() {
        // Try ip-api.com first (free, fast)
        String code = getFromIpApi();
        if (code != null && !code.isEmpty()) {
            return code.toUpperCase();
        }

        // Fallback: try ip.sb with server IP
        String serverIp = getServerIp();
        if (serverIp != null && !serverIp.equals("127.0.0.1")) {
            code = getFromIpSb(serverIp);
            if (code != null && !code.isEmpty()) {
                return code.toUpperCase();
            }
        }

        // Default fallback
        return "XX";
    }

    private static String getFromIpApi() {
        try {
            URL url = new URL(IP_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "curl/7.68.0");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null) {
                        // Response is just the country code, e.g., "US"
                        return line.replace("\"", "").trim();
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.warning("ip-api.com lookup failed: " + e.getMessage());
        }
        return null;
    }

    private static String getFromIpSb(String ip) {
        try {
            URL url = new URL(String.format(IP_SB_URL, ip));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "curl/7.68.0");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // Parse JSON to get country_code
                    String json = response.toString();
                    int start = json.indexOf("\"country_code\"");
                    if (start > 0) {
                        int valueStart = json.indexOf(":", start) + 2;
                        int valueEnd = json.indexOf("\"", valueStart);
                        if (valueStart > 0 && valueEnd > valueStart) {
                            return json.substring(valueStart, valueEnd);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.warning("ip.sb lookup failed: " + e.getMessage());
        }
        return null;
    }

    private static String getServerIp() {
        try {
            Process process = Runtime.getRuntime().exec("curl -s4 ip.sb");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String ip = reader.readLine();
                return ip != null ? ip : "127.0.0.1";
            }
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Get full country name from country code
     */
    public static String getCountryName(String code) {
        return switch (code.toUpperCase()) {
            case "CN" -> "China";
            case "US" -> "United States";
            case "JP" -> "Japan";
            case "KR" -> "South Korea";
            case "SG" -> "Singapore";
            case "HK" -> "Hong Kong";
            case "TW" -> "Taiwan";
            case "DE" -> "Germany";
            case "GB" -> "United Kingdom";
            case "FR" -> "France";
            case "AU" -> "Australia";
            case "CA" -> "Canada";
            case "RU" -> "Russia";
            case "IN" -> "India";
            case "BR" -> "Brazil";
            case "NL" -> "Netherlands";
            case "MY" -> "Malaysia";
            case "TH" -> "Thailand";
            case "VN" -> "Vietnam";
            case "PH" -> "Philippines";
            case "ID" -> "Indonesia";
            default -> code;
        };
    }

    private CountryCodeUtil() {
        throw new IllegalStateException("Utility class");
    }
}
