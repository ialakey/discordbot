package com.alakey.discordbot.bot.config;

import com.alakey.discordbot.bot.listener.VoiceChannelListener;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdaConfig {

    private final VoiceChannelListener voiceChannelListener;

    @Value("${discord.bot.token}")
    private String token;

    @Getter
    private JDA jda;

    @PostConstruct
    public void startBot() throws Exception {
        jda = JDABuilder.createDefault(token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MEMBERS
                )
                .enableCache(CacheFlag.MEMBER_OVERRIDES)
                .addEventListeners(
                        voiceChannelListener
                )
                .setActivity(Activity.watching("на пидоров несчастных"))
                .build();
        jda.awaitReady();
    }
}