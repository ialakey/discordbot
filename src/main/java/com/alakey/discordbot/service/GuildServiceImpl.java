package com.alakey.discordbot.service;

import com.alakey.discordbot.discordbot.config.JdaConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildServiceImpl implements GuildService {

    private final JdaConfig jdaConfig;
    private final DiscordCacheService discordCacheService;

    @Value("${discord.bot.guildById}")
    private String guildId;

    public GuildServiceImpl(JdaConfig jdaConfig, DiscordCacheService discordCacheService) {
        this.jdaConfig = jdaConfig;
        this.discordCacheService = discordCacheService;
    }

    @Override
    public Guild getGuild() {
        return jdaConfig.getJda().getGuildById(guildId);
    }

    @Override
    public VoiceChannel findVoiceChannel(String name) {
        Guild guild = getGuild();
        return guild != null ? guild.getVoiceChannelsByName(name, true).stream().findFirst().orElse(null) : null;
    }

    @Override
    public List<String> getAllVoiceChannels() {
        Guild guild = getGuild();
        return guild == null ? List.of() :
               guild.getVoiceChannels().stream().map(vc -> vc.getName()).toList();
    }

    @Override
    public List<String> getAllRoles() {
        Guild guild = getGuild();
        return guild == null ? List.of() :
               guild.getRoles().stream().map(Role::getName).toList();
    }

    @Override
    public List<String> getAllUsers() {
        discordCacheService.refreshCache();
        return discordCacheService.getCachedUsernames();
    }
}
