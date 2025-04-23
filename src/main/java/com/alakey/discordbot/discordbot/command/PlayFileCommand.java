package com.alakey.discordbot.discordbot.command;

import com.alakey.discordbot.discordbot.audio.AudioPlayerSendHandler;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;

@Slf4j
public class PlayFileCommand implements Command {

    private final File audioFile;
    private final String voiceChannelName;

    public PlayFileCommand(File audioFile, String voiceChannelName) {
        this.audioFile = audioFile;
        this.voiceChannelName = voiceChannelName;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        String targetChannel = (voiceChannelName != null) ? voiceChannelName : "дискорд заебал лагать";

        VoiceChannel channel = event.getGuild()
                .getVoiceChannelsByName(targetChannel, true)
                .stream().findFirst().orElse(null);

        if (channel == null) {
            event.getChannel().sendMessage("Голосовой канал не найден: " + targetChannel).queue();
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

        playerManager.loadItem(absolutePath, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
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
                event.getChannel().sendMessage("Не удалось найти трек.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getChannel().sendMessage("Ошибка загрузки трека: " + exception.getMessage()).queue();
            }
        });
    }
}
