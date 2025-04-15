package com.alakey.discordbot.bot.command;

import com.alakey.discordbot.bot.audio.AudioPlayerSendHandler;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.io.File;

@Slf4j
public class PlayFileCommand implements Command {

    private final File audioFile;

    public PlayFileCommand(File audioFile) {
        this.audioFile = audioFile;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        VoiceChannel channel = event.getGuild()
                .getVoiceChannelsByName("дискорд заебал лагать", true)
                .stream().findFirst().orElse(null);

        if (channel == null) {
            event.getChannel().sendMessage("Не найден голосовой канал.").queue();
            return;
        }

        channel.getGuild().getAudioManager().openAudioConnection(channel);

        try {
            playAudio(channel, audioFile, event);

            event.getMessage().delete().queue(
                    success -> log.info("Команда удалена из чата"),
                    error -> log.warn("Не удалось удалить сообщение", error)
            );

        } catch (Exception e) {
            log.error("Ошибка воспроизведения", e);
            event.getChannel().sendMessage("Ошибка при воспроизведении: " + e.getMessage()).queue();
        }
    }

    private void playAudio(VoiceChannel channel, File audioFile, MessageReceivedEvent event) throws Exception {
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

                track.setUserData(event);

                player.addListener((AudioEventListener) audioEvent -> {
                    if (audioEvent instanceof TrackEndEvent) {
                        event.getGuild().kickVoiceMember(event.getGuild().getSelfMember()).queue(
                                success -> log.info("Бот исключён из голосового канала"),
                                error -> log.error("Ошибка исключения бота", error)
                        );
                    }
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    trackLoaded(playlist.getTracks().get(0));
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
}