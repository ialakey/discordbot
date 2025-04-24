package com.alakey.discordbot.discordbot.command;

import com.alakey.discordbot.discordbot.session.RecordingSession;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class StopCommand implements Command {

    private final String telegramToken;
    private final String chatId;

    public StopCommand(String telegramToken, String chatId) {
        this.telegramToken = telegramToken;
        this.chatId = chatId;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        event.getMessage().delete().queue();

        RecordingSession session = RecordingSession.getInstance();

        if (!session.isActive()) {
            event.getChannel().sendMessage("Нет активной записи.").queue();
            return;
        }

        session.stop();
        File wavFile = session.getWavFile();
        session.clear();

        if (wavFile != null && wavFile.exists()) {
            sendRecordingToTelegram(wavFile);
            event.getChannel().sendMessage("Запись остановлена и отправлена в Telegram.").queue();
        } else {
            event.getChannel().sendMessage("Файл записи не найден.").queue();
        }
    }

    private void sendRecordingToTelegram(File wavFile) {
        try {
            File oggFile = convertWavToOgg(wavFile);

            String url = "https://api.telegram.org/bot" + telegramToken + "/sendVoice";
            HttpRequest.BodyPublisher body = buildMultipartVoiceBody(oggFile);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=----JavaBotBoundary")
                    .POST(body)
                    .build();

            HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        wavFile.delete();
                        oggFile.delete();
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File convertWavToOgg(File wavFile) throws IOException, InterruptedException {
        File oggFile = new File(wavFile.getParent(), wavFile.getName().replace(".wav", ".ogg"));

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", wavFile.getAbsolutePath(),
                "-c:a", "libopus",
                oggFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        if (!oggFile.exists()) {
            throw new IOException("Не удалось сконвертировать WAV в OGG.");
        }

        return oggFile;
    }

    private HttpRequest.BodyPublisher buildMultipartVoiceBody(File file) throws IOException {
        String boundary = "----JavaBotBoundary";
        var byteArray = new StringBuilder();

        byteArray.append("--").append(boundary).append("\r\n");
        byteArray.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
        byteArray.append(chatId).append("\r\n");

        byteArray.append("--").append(boundary).append("\r\n");
        byteArray.append("Content-Disposition: form-data; name=\"voice\"; filename=\"")
                .append(file.getName()).append("\"\r\n");
        byteArray.append("Content-Type: audio/ogg\r\n\r\n");

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] prefix = byteArray.toString().getBytes();
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();

        return HttpRequest.BodyPublishers.ofByteArrays(
                java.util.List.of(prefix, fileBytes, suffix)
        );
    }
}
