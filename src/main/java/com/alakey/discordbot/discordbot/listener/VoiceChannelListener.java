package com.alakey.discordbot.discordbot.listener;

import com.alakey.discordbot.discordbot.command.CommandManager;
import com.alakey.discordbot.service.BlockedEntityService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoiceChannelListener extends ListenerAdapter {

    private final CommandManager commandManager;
    private final BlockedEntityService blockedEntityService;

    public VoiceChannelListener(
            BlockedEntityService blockedEntityService,
            @Value("${bot.audio-folder}") String pathAudio,
            @Value("${telegram.bot.token}") String telegramToken,
            @Value("${telegram.chat.id}") String chatId
    ) {
        this.commandManager = new CommandManager(pathAudio, telegramToken, chatId);
        this.blockedEntityService = blockedEntityService;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Member member = event.getMember();

        event.getGuild().loadMembers().onSuccess(loadedGuild -> {
            if (event.getChannelJoined() != null) {
                String effectiveName = member.getEffectiveName().toLowerCase();
                String username = member.getUser().getName().toLowerCase();

                boolean hasBlockedRole = member.getRoles().stream()
                        .map(role -> role.getName().toLowerCase())
                        .anyMatch(roleName -> blockedEntityService.getBlockedRoles().contains(roleName));

                boolean hasBlockedName = blockedEntityService.getBlockedNames().stream()
                        .map(String::toLowerCase)
                        .anyMatch(blocked -> blocked.equals(effectiveName) || blocked.equals(username));

                if (hasBlockedRole || hasBlockedName) {

                    member.getGuild()
                            .moveVoiceMember(member, null)
                            .queue(
                                    success -> log.info("Исключён из войс-канала: {}", member.getEffectiveName()),
                                    error -> log.error("Ошибка исключения из войса", error)
                            );
                }
            }
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        commandManager.handleCommand(event);
    }
}