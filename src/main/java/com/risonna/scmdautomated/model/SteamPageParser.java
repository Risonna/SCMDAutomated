package com.risonna.scmdautomated.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.HtmlTreeBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SteamPageParser {
    public static Map<String, String> parseSteamPage(String url) {
        Map<String, String> map = new HashMap<>();
        try {
            // Send a GET request to the provided URL
            Document doc = Jsoup.connect(url)
                    .timeout(10000) // set a timeout of 10 seconds
                    .parser(new org.jsoup.parser.Parser(new HtmlTreeBuilder())) // use a faster parser
                    .get();

            //Is collection?
            boolean isCollection = false;
            Element mainContents = doc.selectFirst(".mainContentsCollectionTop");
            if(mainContents!=null){
                isCollection = true;
            }
            if(!isCollection) {
                parseWorkshopItem(map, doc);
                String gamePageUrl = doc.selectFirst(".apphub_OtherSiteInfo").selectFirst(".btnv6_blue_hoverfade").attr("href");
                parseGamePage(map, gamePageUrl);
            }
            else {
                parseCollectionPage(map, doc);
            }






            return map;
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    private static void parseWorkshopItem(Map<String, String> map, Document doc){
        // Extract the title
        Element titleElement = doc.selectFirst(".workshopItemTitle");
        if (titleElement!= null) {
            String title = titleElement.text();
            System.out.println("Title: " + title);
            map.put("title", title);
        } else {
            System.out.println("Error: Title not found");
        }

        // Extract the image URL
        Element imageElement = doc.selectFirst("#previewImageMain");
        if (imageElement!= null) {
            String imageUrl = imageElement.attr("src");
            System.out.println("Image URL: " + imageUrl);
            map.put("image", imageUrl);
        } else {
            System.out.println("Error: Image not found");
        }

        //Extract game name
        Element gameElement = doc.selectFirst(".apphub_AppName");
        if (gameElement!= null) {
            String gameName = gameElement.text();
            System.out.println("game name is :" + gameName);
            map.put("gameName", gameName);
        }

        Element rightPanelElement = doc.selectFirst(".detailsStatsContainerRight");

        //Extract item size
        Element itemSizeElement = rightPanelElement.selectFirst(".detailsStatRight");
        String elementSize = itemSizeElement.text();
        if(elementSize != null){
            map.put("itemSize", elementSize);
        }

        //Extract item releaseDate and
        int i = 0;
        String itemReleaseDate = null;
        String itemUpdateDate = null;

        for (Element element : rightPanelElement.children()){
            if(i==0){
                i++;
                continue;
            };
            if(i==1){itemReleaseDate = element.text();}
            if(i==2){itemUpdateDate = element.text();}
            i++;
        }

        if(itemReleaseDate != null && itemUpdateDate != null){
            map.put("itemReleaseDate", itemReleaseDate);
            map.put("itemUpdateDate", itemUpdateDate);
        }
    }
    private static void parseGamePage(Map<String, String> map, String url){
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(10000) // set a timeout of 10 seconds
                    .parser(new org.jsoup.parser.Parser(new HtmlTreeBuilder())) // use a faster parser
                    .get();
            Element releaseDate = doc.selectFirst(".date");
            String releaseDateText = releaseDate!= null? releaseDate.text() : null;
            if(releaseDateText != null){
                map.put("gameReleaseDate", releaseDateText);
            }

            Element gameImage = doc.selectFirst(".game_header_image_full");
            String gameImageUrl = gameImage!= null? gameImage.attr("src") : null;
            if(gameImageUrl != null){
                map.put("gameImageUrl", gameImageUrl);
             }

        } catch (IOException e) {
            System.out.println(e);
        }

    }

    private static void parseCollectionPage(Map<String, String> map, Document doc){

    }
}