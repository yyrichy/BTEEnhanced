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

public class UpdateChecker implements Runnable{
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
        String latestVersion;
        int code = 0;
        try {
            URL url = new URL("https://api.github.com/repos/vaporrrr/bteenhanced/releases");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            JsonArray jsonArray;
            try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                jsonArray = new JsonParser().parse(response.toString()).getAsJsonArray();
            }
            code = con.getResponseCode();
            latestVersion = cleanVersion(jsonArray.get(0).getAsJsonObject().get("tag_name").getAsString());
        } catch(Exception e) {
            logger.severe("Could not get latest version number.");
            if (code != 0) {
                logger.severe("Response code: " + code);
            }
            e.printStackTrace();
            latestVersion = "NOT FOUND";
        }
        return latestVersion;
    }

    private String getCurrentVersion() {
        return cleanVersion(bteEnhanced.getDescription().getVersion());
    }

    private String cleanVersion(String version) {
        return version.replaceAll("[^0-9.]", "");
    }
}
