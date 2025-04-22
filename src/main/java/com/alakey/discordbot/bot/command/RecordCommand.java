package com.alakey.discordbot.bot.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.util.concurrent.*;

public class RecordCommand implements Command {

    private static final int RECORD_SECONDS = 10;

    private final String telegramToken;
    private final String chatId;

    public RecordCommand(String telegramToken, String chatId) {
        this.telegramToken = telegramToken;
        this.chatId = chatId;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        event.getMessage().delete().queue();

        String[] parts = event.getMessage().getContentRaw().split(" ", 2);
        if (parts.length < 2) {
            event.getChannel().sendMessage("Укажи имя голосового канала: `!record name_channel`").queue();
            return;
        }

        String channelName = parts[1].trim();
        Guild guild = event.getGuild();
        AudioChannel voiceChannel = guild.getVoiceChannels().stream()
                .filter(vc -> vc.getName().equalsIgnoreCase(channelName))
                .findFirst()
                .orElse(null);

        if (voiceChannel == null) {
            event.getChannel().sendMessage("Канал не найден: " + channelName).queue();
            return;
        }

        AudioManager audioManager = guild.getAudioManager();
        MessageChannel textChannel = event.getChannel();

        String directoryPath = "resources";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String filePath = directoryPath + "/recording_" + System.currentTimeMillis() + ".wav";

        SimpleAudioRecorder recorder = new SimpleAudioRecorder(filePath, textChannel);
        audioManager.setReceivingHandler(recorder);
        audioManager.openAudioConnection(voiceChannel);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            audioManager.closeAudioConnection();
            recorder.close();

            File wavFile = new File(filePath);
            sendRecordingToTelegram(wavFile);

        }, RECORD_SECONDS, TimeUnit.SECONDS);
    }

    private void sendRecordingToTelegram(File audioFile) {
        try {
            String url = "https://api.telegram.org/bot" + telegramToken + "/sendDocument";

            HttpRequest.BodyPublisher body = buildMultipartBody(audioFile);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=----JavaBotBoundary")
                    .POST(body)
                    .build();

            HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Telegram ответ: " + response.body());
                        audioFile.delete();
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HttpRequest.BodyPublisher buildMultipartBody(File file) throws IOException {
        String boundary = "----JavaBotBoundary";
        var byteArray = new StringBuilder();

        byteArray.append("--").append(boundary).append("\r\n");
        byteArray.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
        byteArray.append(chatId).append("\r\n");

        byteArray.append("--").append(boundary).append("\r\n");
        byteArray.append("Content-Disposition: form-data; name=\"document\"; filename=\"")
                .append(file.getName()).append("\"\r\n");
        byteArray.append("Content-Type: audio/wav\r\n\r\n");

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] prefix = byteArray.toString().getBytes();
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();

        return HttpRequest.BodyPublishers.ofByteArrays(
                java.util.List.of(prefix, fileBytes, suffix)
        );
    }
}
