package com.alakey.discordbot.telegrambot;

import com.alakey.discordbot.service.VoiceChannelInfoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Timer;
import java.util.TimerTask;

public class TelegramBotPoller {

    private final String apiUrl;
    private final VoiceChannelInfoService infoService;
    private long lastUpdateId = 0;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramBotPoller(String telegramToken, VoiceChannelInfoService infoService) {
        this.infoService = infoService;
        this.apiUrl = "https://api.telegram.org/bot" + telegramToken + "/";
    }

    public void start() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    pollUpdates();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2000);
    }

    private void pollUpdates() throws IOException, InterruptedException {
        String url = apiUrl + "getUpdates?offset=" + (lastUpdateId + 1);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        for (JsonNode update : root.path("result")) {
            lastUpdateId = update.path("update_id").asLong();

            JsonNode message = update.path("message");
            if (message.isMissingNode()) continue;

            String text = message.path("text").asText();
            long chatId = message.path("chat").path("id").asLong();

            if (text.equals("/vcinfo")) {
                String info = infoService.getDetailedVoiceChannelInfo();
                sendMessage(chatId, info);
            }
        }
    }

    private void sendMessage(long chatId, String messageText) {
        try {
            String payload = """
                {
                  "chat_id": %d,
                  "text": "%s",
                  "parse_mode": "Markdown"
                }
                """.formatted(chatId, escape(messageText));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "sendMessage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escape(String text) {
        return text.replace("\"", "\\\"")
                   .replace("\n", "\\n");
    }
}
