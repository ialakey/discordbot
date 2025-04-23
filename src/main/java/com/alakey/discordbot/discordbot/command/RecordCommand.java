package com.alakey.discordbot.discordbot.command;

import com.alakey.discordbot.discordbot.session.RecordingSession;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;

public class RecordCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event) {
        event.getMessage().delete().queue();

        String[] parts = event.getMessage().getContentRaw().split(" ", 2);
        if (parts.length < 2) {
            event.getChannel().sendMessage("Укажи имя голосового канала: `!record name_channel`").queue();
            return;
        }

        if (RecordingSession.getInstance().isActive()) {
            event.getChannel().sendMessage("Уже идёт запись. Останови её через `!stop`.").queue();
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

        String filePath = "resources/recording_" + System.currentTimeMillis() + ".wav";
        File wavFile = new File(filePath);

        SimpleAudioRecorder recorder = new SimpleAudioRecorder(filePath, textChannel);
        audioManager.setReceivingHandler(recorder);
        audioManager.openAudioConnection(voiceChannel);

        RecordingSession.getInstance().start(recorder, null, audioManager, wavFile);

        textChannel.sendMessage("Запись начата в канале: " + channelName + ". Останови командой `!stop`.").queue();
    }
}
