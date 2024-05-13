package com.example;

// imports for app
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import org.json.JSONObject;

// imports for github/quality of life
import io.github.cdimascio.dotenv.Dotenv;

// base class for making API requests
class BungieAPI {
    protected static final Dotenv dotenv = Dotenv.load();
    protected static final String API_KEY = dotenv.get("BUNGIE_API_KEY");
    protected HttpClient client = HttpClient.newHttpClient();

    protected JSONObject makeApiRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", API_KEY)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }
}

// class for fetching PvP stats
class PvPStats extends BungieAPI {
    public JSONObject fetchPvPStats(String membershipType, String destinyMembershipId) throws IOException, InterruptedException {
        String endpoint = String.format("https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/0/Stats/",
                membershipType, destinyMembershipId);
        return makeApiRequest(endpoint);
    }

    public void provideGameplaySuggestions(JSONObject stats) {
            // narrow json by plaver vs player stats 
            JSONObject allPvP = stats.getJSONObject("Response").getJSONObject("allPvP").getJSONObject("allTime");
    
            // store stats of interest
            double averageKillDistance = allPvP.getJSONObject("averageKillDistance").getJSONObject("basic").getDouble("value");
            double averageLifespan = allPvP.getJSONObject("averageLifespan").getJSONObject("basic").getDouble("value");
            double killsDeathsRatio = allPvP.getJSONObject("killsDeathsRatio").getJSONObject("basic").getDouble("value");
            double winLossRatio = allPvP.getJSONObject("winLossRatio").getJSONObject("basic").getDouble("value");
        
            // display data
            System.out.println("Gameplay Statistics:");
            System.out.println("Average Kill Distance: " + averageKillDistance);
            System.out.println("Average Lifespan: " + averageLifespan);
            System.out.println("Kills/Deaths Ratio: " + killsDeathsRatio);
            System.out.println("Win/Loss Ratio: " + winLossRatio);
    
            // suggest things based off strengths and weaknesses
            if (averageKillDistance < 10) {
                System.out.println("Try to engage at longer distances to improve safety and efficiency.");
            } else if (averageKillDistance >= 10 && averageKillDistance <= 20) {
                System.out.println("Your engagement distance is average, consider practicing your precision aiming.");
            } else {
                System.out.println("Great job on maintaining a safe engagement distance!");
            }
    
            if (averageLifespan < 60) {
                System.out.println("Work on your positioning and map awareness to stay alive longer.");
            } else if (averageLifespan >= 60 && averageLifespan <= 120) {
                System.out.println(
                        "You have a decent lifespan. Consider tweaking your gear or abilities for better survival.");
            } else {
                System.out.println("Excellent survival skills!");
            }
    
            if (killsDeathsRatio < 1.0) {
                System.out.println("Focus on reducing deaths by playing more conservatively.");
            } else if (killsDeathsRatio >= 1.0 && killsDeathsRatio <= 1.5) {
                System.out
                        .println("Your K/D is solid! Try to push for more aggressive plays safely to increase it further.");
            } else {
                System.out.println(
                        "Excellent K/D ratio! Keep up the aggressive play while maintaining your current strategy.");
            }
    
            if (winLossRatio < 0.5) {
                System.out.println(
                        "Your win rate is quite low; consider teaming up with friends or other players to improve teamwork.");
            } else if (winLossRatio >= 0.5 && winLossRatio <= 1.0) {
                System.out.println(
                        "You have an average win rate. Focus on strategic plays and objective control to improve your win rate.");
            } else {
                System.out.println("Great win rate! Your team play and strategy are paying off well.");
            }
    }
    
}

// Class for fetching player's username
class PlayerInfo extends BungieAPI {
    public String fetchPlayerUsername(String membershipType, String destinyMembershipId) throws IOException, InterruptedException {
        String url = String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=100",
                membershipType, destinyMembershipId);
        JSONObject response = makeApiRequest(url);
        return response.getJSONObject("Response").getJSONObject("profile").getJSONObject("data").getJSONObject("userInfo").getString("bungieGlobalDisplayName");
    }
}

public class Destiny2PvPStatsAdvisor {

    public static void main(String[] args) {
        
        // print key to check env & other class works, debugging
        System.out.println("API Key: " + BungieAPI.API_KEY);
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter your Bungie.net Destiny 2 profile URL:");
        String inputUrl = scanner.nextLine();

        // get player's account info from scanner, store to string array
        try {
            String[] extractedParams = extractParameters(inputUrl);
            String membershipType = extractedParams[0];
            String destinyMembershipId = extractedParams[1];

            // fetch the player's username using the PlayerInfo class
            PlayerInfo playerInfo = new PlayerInfo();
            String username = playerInfo.fetchPlayerUsername(membershipType, destinyMembershipId);
            System.out.println("Player Username: " + username);
            
            // create an instance of PvPStats to fetch PvP statistics
            PvPStats pvpStats = new PvPStats();
            JSONObject stats = pvpStats.fetchPvPStats(membershipType, destinyMembershipId);
            pvpStats.provideGameplaySuggestions(stats);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    // Method to extract parameters from the URL provided by the user
    private static String[] extractParameters(String url) {
        String pattern = "https://www\\.bungie\\.net/\\d+/en/User/Profile/(\\d+)/(\\d+)";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(url);

        if (m.find()) {
            return new String[] { m.group(1), m.group(2) };
        } else {
            throw new IllegalArgumentException("Invalid URL format");
        }
    }
}