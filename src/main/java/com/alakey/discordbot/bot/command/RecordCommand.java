package com.alakey.discordbot.bot.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.concurrent.*;

public class RecordCommand implements Command {

    private static final int RECORD_SECONDS = 30;

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

        String directoryPath = "D:/MyApps/discordbot/src/main/resources";
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

            sendRecordingFileToDiscord(new File(filePath), textChannel);

        }, RECORD_SECONDS, TimeUnit.SECONDS);
    }

    private void sendRecordingFileToDiscord(File audioFile, MessageChannel textChannel) {
        textChannel.sendFiles(FileUpload.fromData(audioFile)).queue(
                success -> {
                    if (audioFile.delete()) {
                        System.out.println("Файл удалён: " + audioFile.getName());
                    }
                },
                failure -> {
                    System.out.println("Ошибка при отправке файла: " + failure.getMessage());
                }
        );
    }
}