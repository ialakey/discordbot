package com.alakey.discordbot.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public class VoiceChannelInfoService {

    private final JDA jda;

    public VoiceChannelInfoService(JDA jda) {
        this.jda = jda;
    }

    public String getDetailedVoiceChannelInfo() {
        StringBuilder result = new StringBuilder("🎧 *Активные голосовые каналы:*\n\n");

        for (Guild guild : jda.getGuilds()) {
            result.append("📌 *").append(guild.getName()).append("*\n");

            var voiceChannels = guild.getVoiceChannels();
            if (voiceChannels.isEmpty()) {
                result.append("  — Нет голосовых каналов\n\n");
                continue;
            }

            for (var vc : voiceChannels) {
                var members = vc.getMembers();
                if (members.isEmpty()) continue;

                result.append("\uD83E\uDD8D ").append(vc.getName()).append(" — ")
                        .append(members.size()).append(" участник(ов):\n");

                for (var member : members) {
                    var voiceState = member.getVoiceState();
                    boolean muted = voiceState.isSelfMuted() || voiceState.isMuted();

                    String micIcon = muted ? "🔇" : "🎤";
                    result.append("   ")
                            .append(member.getEffectiveName())
                            .append(" ")
                            .append(micIcon)
                            .append("\n");
                }
                result.append("\n");
            }
        }

        return result.toString();
    }
}
