package com.github.vaporrrr.bteenhanced;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class UpdateChecker implements Runnable {
    private final BTEEnhanced bteEnhanced;
    private final Logger logger;

    public UpdateChecker(BTEEnhanced bteEnhanced) {
        this.bteEnhanced = bteEnhanced;
        this.logger = bteEnhanced.getLogger();
    }

    @Override
    public void run() {
        logger.info(ChatColor.GRAY + "-----CHECKING FOR UPDATES-----");
        String current = getCurrentVersion();
        String latest = getLatestVersion();
        logger.info(ChatColor.AQUA + "Current version: " + current);
        logger.info(ChatColor.AQUA + "Latest version: " + latest);
        if (!current.equals(latest)) {
            logger.info(ChatColor.DARK_RED + "Plugin is not latest! Is it outdated? https://github.com/vaporrrr/BTEEnhanced/releases");
        } else {
            logger.info(ChatColor.GREEN + "Plugin is up to date.");
        }
        logger.info(ChatColor.GRAY + "------------------------------");
    }

    private String getLatestVersion() {
        String latestVersion = "NOT FOUND";
        try {
            URL url = new URL("https://api.github.com/repos/vaporrrr/bteenhanced/releases");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            JsonArray jsonArray;
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            int code = con.getResponseCode();
            if (isSuccessStatusCode(code)) {
                jsonArray = JsonParser.parseString(response.toString()).getAsJsonArray();
                latestVersion = cleanVersion(jsonArray.get(0).getAsJsonObject().get("tag_name").getAsString());
            } else {
                logger.severe("Request for latest release not successful. Response code: " + code);
            }
        } catch (Exception e) {
            logger.severe("Unexpected error occurred while getting latest release version number.");
            e.printStackTrace();
        }
        return latestVersion;
    }

    private String getCurrentVersion() {
        return cleanVersion(bteEnhanced.getDescription().getVersion());
    }

    private static String cleanVersion(String version) {
        return version.replaceAll("[^0-9.]", "");
    }

    private static boolean isSuccessStatusCode(int code) {
        return code >= 200 && code <= 299;
    }
}
