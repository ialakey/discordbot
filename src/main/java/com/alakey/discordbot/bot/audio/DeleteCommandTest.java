package com.alakey.discordbot.bot.audio;

import com.alakey.discordbot.bot.command.Command;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

@Slf4j
public class DeleteCommandTest implements Command {

    @Value("${bot.audio-folder}")
    private String pathAudio;

    @Override
    public void execute(MessageReceivedEvent event) {
        File audioFile = new File(pathAudio + "expansion_of_territory.mp3");

        if (!audioFile.exists()) {
            event.getChannel().sendMessage("Файл не найден: expansion_of_territory.mp3").queue();
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

        playAudioAndKickAfter(channel, audioFile, event);
    }

    private void playAudioAndKickAfter(VoiceChannel channel, File audioFile, MessageReceivedEvent event) {
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
                            log.info("Трек завершён. Начинаем удаление участников...");
                            kickMembers(channel);
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

    private void kickMembers(VoiceChannel channel) {
        for (Member member : channel.getMembers()) {
            if (!member.getUser().isBot()) {
                channel.getGuild().kickVoiceMember(member).queue(
                        success -> log.info("Исключён: {}", member.getEffectiveName()),
                        error -> log.error("Ошибка исключения пользователя: {}", member.getEffectiveName(), error)
                );
            }
        }

        channel.getGuild().kickVoiceMember(channel.getGuild().getSelfMember()).queue(
                success -> log.info("Бот исключён из канала"),
                error -> log.error("Ошибка исключения бота", error)
        );
    }
}