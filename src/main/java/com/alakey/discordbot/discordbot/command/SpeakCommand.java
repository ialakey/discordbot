package com.alakey.discordbot.discordbot.command;

import com.alakey.discordbot.discordbot.audio.AudioPlayerSendHandler;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.tetradotoxina.gtts4j.GTTS4J;
import com.tetradotoxina.gtts4j.exception.GTTS4JException;
import com.tetradotoxina.gtts4j.impl.GTTS4JImpl;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.IOException;

@Slf4j
public class SpeakCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event) {
        String messageContent = event.getMessage().getContentRaw();
        String[] parts = messageContent.split(" ", 3);

        if (parts.length < 3) {
            event.getChannel().sendMessage("Использование команды: !speak <канал> <текст>").queue();
            return;
        }

        String channelName = parts[1].trim();
        String textToSpeak = parts[2].trim();

        if (textToSpeak.isEmpty()) {
            event.getChannel().sendMessage("Пожалуйста, укажите текст для озвучивания!").queue();
            return;
        }

        VoiceChannel channel = event.getGuild()
                .getVoiceChannelsByName(channelName, true)
                .stream().findFirst().orElse(null);

        if (channel == null) {
            event.getChannel().sendMessage("Не найден голосовой канал с названием: " + channelName).queue();
            return;
        }

        channel.getGuild().getAudioManager().openAudioConnection(channel);

        event.getMessage().delete().queue(
                success -> log.info("Команда удалена из чата"),
                error -> log.warn("Не удалось удалить сообщение", error)
        );

        File audioFile = synthesizeWithGtts(textToSpeak);

        playAudioAndLeaveAfter(channel, audioFile, event);
    }

    private File synthesizeWithGtts(String text) {
        try {
            GTTS4J gtts4j = new GTTS4JImpl();
            String lang = "ru";
            boolean slow = false;

            byte[] data = gtts4j.textToSpeech(text, lang, slow);

            File tempMp3 = File.createTempFile("tts_audio", ".mp3");
            gtts4j.saveFile(tempMp3.getAbsolutePath(), data, true);

            return tempMp3;
        } catch (IOException | GTTS4JException e) {
            throw new RuntimeException("Ошибка при синтезе речи через gtts4j", e);
        }
    }

    private void playAudioAndLeaveAfter(VoiceChannel channel, File audioFile, MessageReceivedEvent event) {
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        channel.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        String absolutePath = audioFile.getAbsolutePath();

        playerManager.loadItem(absolutePath, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);

                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            log.info("Трек завершён. Бот покидает канал.");
                            leaveVoiceChannel(channel);
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
                event.getChannel().sendMessage("Не удалось найти трек.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getChannel().sendMessage("Ошибка загрузки трека: " + exception.getMessage()).queue();
            }
        });
    }

    private void leaveVoiceChannel(VoiceChannel channel) {
        channel.getGuild().getAudioManager().closeAudioConnection();
    }
}