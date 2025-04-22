package com.alakey.discordbot.service;

import com.alakey.discordbot.bot.config.JdaConfig;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DiscordCacheService {

    @Value("${discord.bot.guildById}")
    private String guildById;
    private final List<String> cachedUsernames = new ArrayList<>();
    private final JdaConfig jdaConfig;

    public DiscordCacheService(JdaConfig jdaConfig) {
        this.jdaConfig = jdaConfig;
    }

    @PostConstruct
    public void loadUsers() {
        Guild guild = jdaConfig.getJda().getGuildById(guildById);
        if (guild != null) {
            guild.loadMembers().onSuccess(members -> {
                cachedUsernames.clear();
                for (Member member : members) {
                    if (!member.getUser().isBot()) {
                        cachedUsernames.add(member.getUser().getName());
                    }
                }
            }).onError(Throwable::printStackTrace);
        }
    }

    public List<String> getCachedUsernames() {
        return cachedUsernames;
    }

    public void refreshCache() {
        Guild guild = jdaConfig.getJda().getGuildById(guildById);
        if (guild != null) {
            guild.loadMembers().onSuccess(members -> {
                cachedUsernames.clear();
                for (Member member : members) {
                    if (!member.getUser().isBot()) {
                        cachedUsernames.add(member.getUser().getName());
                    }
                }
            }).onError(Throwable::printStackTrace);
        }
    }
}
