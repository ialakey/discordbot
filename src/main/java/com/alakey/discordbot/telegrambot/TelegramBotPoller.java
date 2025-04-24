package com.alakey.discordbot.telegrambot;

import com.alakey.discordbot.discordbot.audio.AudioPlayerSendHandler;
import com.alakey.discordbot.service.VoiceChannelInfoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class TelegramBotPoller {

    private final String apiUrl;
    private final VoiceChannelInfoService infoService;
    private final Guild discordGuild;
    private long lastUpdateId = 0;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramBotPoller(String telegramToken, VoiceChannelInfoService infoService, Guild discordGuild) {
        this.infoService = infoService;
        this.discordGuild = discordGuild;
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

            String text = message.path("text").asText("");
            long chatId = message.path("chat").path("id").asLong();

            if (text.equals("/vcinfo")) {
                String info = infoService.getDetailedVoiceChannelInfo();
                sendMessage(chatId, info);
                deleteMessage(chatId, message.path("message_id").asInt());
            }

            if (text.startsWith("/sendvoice")) {
                String[] parts = text.split(" ", 2);
                if (parts.length < 2) {
                    sendMessage(chatId, "Неверный формат команды. Используйте: /sendvoice <имя_канала> (в ответ на голосовое сообщение)");
                    deleteMessage(chatId, message.path("message_id").asInt());
                    continue;
                }
                String channelName = parts[1];

                JsonNode reply = message.path("reply_to_message");
                if (reply.isMissingNode()) {
                    sendMessage(chatId, "Вы должны ответить на голосовое сообщение этой командой.");
                    continue;
                }

                JsonNode voice = reply.path("voice");
                if (voice.isMissingNode()) {
                    sendMessage(chatId, "Ответ должно быть на голосовое сообщение.");
                    continue;
                }

                String fileId = voice.path("file_id").asText();

                sendVoiceMessageToDiscord(chatId, channelName, fileId);
                deleteMessage(chatId, message.path("message_id").asInt());
            }
        }
    }

    private void sendVoiceMessageToDiscord(long chatId, String channelName, String fileId) {
        try {
            String encodedFileId = URLEncoder.encode(fileId, StandardCharsets.UTF_8);
            String fileUrl = apiUrl + "getFile?file_id=" + encodedFileId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String filePath = root.path("result").path("file_path").asText();

            String audioUrl = "https://api.telegram.org/file/bot" + apiUrl.substring(apiUrl.indexOf("bot") + 3, apiUrl.length() - 1) + "/" + filePath;

            VoiceChannel voiceChannel = discordGuild.getVoiceChannelsByName(channelName, true).stream().findFirst().orElse(null);
            if (voiceChannel == null) {
                sendMessage(chatId, "Голосовой канал не найден.");
                return;
            }

            voiceChannel.getGuild().getAudioManager().openAudioConnection(voiceChannel);
            playAudioAndLeaveAfter(voiceChannel, audioUrl);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка при отправке голосового сообщения.");
        }
    }

    private void playAudioAndLeaveAfter(VoiceChannel channel, String audioUrl) {
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        channel.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        playerManager.loadItemOrdered(player, audioUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            channel.getGuild().getAudioManager().closeAudioConnection();
                        }
                    }
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    player.playTrack(playlist.getTracks().get(0));
                }
            }

            @Override
            public void noMatches() {
                System.out.println("Не удалось загрузить аудио.");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.out.println("Ошибка загрузки аудио: " + exception.getMessage());
            }
        });
    }

    private void sendMessage(long chatId, String messageText) {
        try {
            String payload = """
                {
                  \"chat_id\": %d,
                  \"text\": \"%s\",
                  \"parse_mode\": \"Markdown\"
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

    private void deleteMessage(long chatId, int messageId) {
        try {
            String payload = """
            {
              \"chat_id\": %d,
              \"message_id\": %d
            }
            """.formatted(chatId, messageId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "deleteMessage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}