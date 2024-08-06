package com.risonna.scmdautomated.model;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class APICaller {

    public static String sendPostRequestCheckItemDetails(List<Long> publishedFileIds) {
        try {
            URL url = new URL("https://api.steampowered.com/ISteamRemoteStorage/GetPublishedFileDetails/v1/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            StringBuilder paramsBuilder = new StringBuilder();
            paramsBuilder.append("itemcount=").append(publishedFileIds.size());
            for (int i = 0; i < publishedFileIds.size(); i++) {
                paramsBuilder.append("&publishedfileids[").append(i).append("]=").append(publishedFileIds.get(i));
            }
            String params = paramsBuilder.toString();

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(params);
            out.flush();
            out.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                System.out.println("Failed to send POST request. Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error sending POST request: " + e.getMessage());
            return null;
        }
    }
    public static String sendGetRequest(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine())!= null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                System.out.println("Failed to send GET request. Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error sending GET request: " + e.getMessage());
            return null;
        }
    }
    public static String sendPostCollectionInfoRequest(String apiUrl, String postData) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(postData.getBytes());
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine())!= null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                System.out.println("Failed to send POST request. Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error sending POST request: " + e.getMessage());
            return null;
        }
    }
}
