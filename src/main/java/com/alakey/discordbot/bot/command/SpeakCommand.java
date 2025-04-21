package com.alakey.discordbot.bot.command;

import com.alakey.discordbot.bot.audio.AudioPlayerSendHandler;
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
        String textToSpeak = messageContent.replace("!speak ", "").trim();

        if (textToSpeak.isEmpty()) {
            event.getChannel().sendMessage("Пожалуйста, укажите текст для озвучивания!").queue();
            return;
        }

        VoiceChannel channel = event.getGuild()
                .getVoiceChannelsByName("дискорд заебал лагать", true)
                .stream().findFirst().orElse(null);

        if (channel == null) {
            event.getChannel().sendMessage("Не найден голосовой канал.").queue();
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

            log.info("Аудиофайл сохранён: {}", tempMp3.getAbsolutePath());

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
        log.info("Загружаем файл (abs path): {}", absolutePath);

        playerManager.loadItem(absolutePath, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                log.info("Трек загружен: {}", track.getInfo().title);
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
                log.info("Плейлист загружен: {}", playlist.getName());
                if (!playlist.getTracks().isEmpty()) {
                    player.playTrack(playlist.getTracks().get(0));
                }
            }

            @Override
            public void noMatches() {
                log.warn("Lavaplayer не нашел трек");
                event.getChannel().sendMessage("Не удалось найти трек.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                log.error("Ошибка загрузки трека", exception);
                event.getChannel().sendMessage("Ошибка загрузки трека: " + exception.getMessage()).queue();
            }
        });
    }

    private void leaveVoiceChannel(VoiceChannel channel) {
        channel.getGuild().getAudioManager().closeAudioConnection();
        log.info("Бот покинул канал {}", channel.getName());
    }
}