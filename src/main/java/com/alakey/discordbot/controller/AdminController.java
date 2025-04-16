package com.alakey.discordbot.controller;

import com.alakey.discordbot.bot.audio.AudioPlayerSendHandler;
import com.alakey.discordbot.service.BlockedEntityService;
import com.alakey.discordbot.service.DiscordCacheService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import com.alakey.discordbot.bot.config.JdaConfig;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Value("${discord.bot.guildById}")
    private String guildById;
    private final JdaConfig jdaConfig;
    private final BlockedEntityService blockedEntityService;

    private final DiscordCacheService discordCacheService;

    public AdminController(JdaConfig jdaConfig, BlockedEntityService blockedEntityService, DiscordCacheService discordCacheService) {
        this.jdaConfig = jdaConfig;
        this.blockedEntityService = blockedEntityService;
        this.discordCacheService = discordCacheService;
    }

    private Guild getGuild() {
        return jdaConfig.getJda().getGuildById(guildById);
    }

    @GetMapping
    public String adminPanel(Model model) {
        model.addAttribute("voiceChannels", getAllVoiceChannels());
        model.addAttribute("audioFiles", getAudioFiles());

        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());

        List<String> discordUsers = getAllDiscordUsers();
        List<String> discordRoles = getAllDiscordRoles();

        model.addAttribute("discordUsers", discordUsers);
        model.addAttribute("discordRoles", discordRoles);

        return "admin";
    }

    @PostMapping("/addBlockedRole")
    public String addBlockedRole(@RequestParam String roleName, Model model) {
        blockedEntityService.addBlockedRole(roleName);

        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());

        return "admin";
    }

    @PostMapping("/addBlockedUser")
    public String addBlockedUser(@RequestParam String username, Model model) {
        blockedEntityService.addBlockedName(username);
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
        return "admin";
    }

    @PostMapping("/removeBlockedUser")
    public String removeBlockedUser(@RequestParam String username, Model model) {
        blockedEntityService.removeBlockedName(username);
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
        return "admin";
    }

    @PostMapping("/removeBlockedRole")
    public String removeBlockedRole(@RequestParam String roleName, Model model) {
        blockedEntityService.removeBlockedRole(roleName);
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
        return "admin";
    }

    @GetMapping("/getAllDiscordUsers")
    public List<String> getAllDiscordUsers() {
        discordCacheService.refreshCache();
        return discordCacheService.getCachedUsernames();
    }

    @GetMapping("/getAllDiscordRoles")
    public List<String> getAllDiscordRoles() {
        List<String> rolesList = new ArrayList<>();
        Guild guild = getGuild();
        if (guild != null) {
            for (Role role : guild.getRoles()) {
                rolesList.add(role.getName());
            }
        }
        return rolesList;
    }

    @GetMapping("/getAllVoiceChannels")
    public List<String> getAllVoiceChannels() {
        Guild guild = getGuild();
        if (guild == null) return List.of();
        return guild.getVoiceChannels().stream().map(vc -> vc.getName()).toList();
    }

    @PostMapping("/playAndKick")
    public String playAudioAndKick(@RequestParam String voiceChannelName, Model model) {
        Guild guild = getGuild();
        if (guild == null) {
            model.addAttribute("error", "Гильдия не найдена.");
            return "admin";
        }

        VoiceChannel channel = guild.getVoiceChannelsByName(voiceChannelName, true)
                .stream().findFirst().orElse(null);

        if (channel == null) {
            model.addAttribute("error", "Голосовой канал не найден: " + voiceChannelName);
            return "admin";
        }

        File audioFile = new File("src/main/resources/audio/csgo.mp3");
        if (!audioFile.exists()) {
            model.addAttribute("error", "Аудиофайл не найден.");
            return "admin";
        }

        guild.getAudioManager().openAudioConnection(channel);

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        playerManager.loadItem(audioFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            for (Member member : channel.getMembers()) {
                                if (!member.getUser().isBot()) {
                                    guild.kickVoiceMember(member).queue();
                                }
                            }

                            guild.kickVoiceMember(guild.getSelfMember()).queue();
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
                log.warn("Трек не найден.");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                log.error("Ошибка загрузки трека", exception);
            }
        });

        model.addAttribute("message", "Воспроизведение запущено.");

        model.addAttribute("voiceChannels", getAllVoiceChannels());
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());

        return "admin";
    }

    @GetMapping("/getAudioFiles")
    public List<String> getAudioFiles() {
        File audioDir = new File("src/main/resources/audio/");
        if (!audioDir.exists() || !audioDir.isDirectory()) return List.of();

        File[] files = audioDir.listFiles((dir, name) -> name.endsWith(".mp3"));
        if (files == null) return List.of();

        List<String> fileNames = new ArrayList<>();
        for (File file : files) {
            fileNames.add(file.getName());
        }

        return fileNames;
    }

    @PostMapping("/playAudioFile")
    public String playAudioFile(
            @RequestParam String voiceChannelName,
            @RequestParam String audioFileName,
            Model model) {

        Guild guild = getGuild();
        if (guild == null) {
            model.addAttribute("error", "Гильдия не найдена");
            populateModel(model);
            return "admin";
        }

        VoiceChannel channel = guild.getVoiceChannelsByName(voiceChannelName, true)
                .stream().findFirst().orElse(null);
        if (channel == null) {
            model.addAttribute("error", "Канал не найден: " + voiceChannelName);
            populateModel(model);
            return "admin";
        }

        File audioFile = new File("src/main/resources/audio/" + audioFileName);
        if (!audioFile.exists()) {
            model.addAttribute("error", "Файл не найден: " + audioFileName);
            populateModel(model);
            return "admin";
        }

        guild.getAudioManager().openAudioConnection(channel);

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        playerManager.loadItem(audioFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            guild.kickVoiceMember(guild.getSelfMember()).queue();
                        }
                    }
                });
            }

            @Override public void playlistLoaded(AudioPlaylist playlist) {}
            @Override public void noMatches() { log.warn("Файл не найден"); }
            @Override public void loadFailed(FriendlyException exception) { log.error("Ошибка загрузки", exception); }
        });

        model.addAttribute("message", "Аудио воспроизводится");
        populateModel(model);
        return "admin";
    }

    private void populateModel(Model model) {
        model.addAttribute("voiceChannels", getAllVoiceChannels());
        model.addAttribute("audioFiles", getAudioFiles());
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
    }

    @PostMapping("/speakText")
    public String speakText(
            @RequestParam String voiceChannelName,
            @RequestParam String textToSpeak,
            Model model
    ) {
        Guild guild = getGuild();
        if (guild == null) {
            model.addAttribute("error", "Гильдия не найдена.");
            populateModel(model);
            return "admin";
        }

        VoiceChannel channel = guild.getVoiceChannelsByName(voiceChannelName, true)
                .stream().findFirst().orElse(null);
        if (channel == null) {
            model.addAttribute("error", "Канал не найден: " + voiceChannelName);
            populateModel(model);
            return "admin";
        }

        File audioFile = synthesizeWithSapiTTS(textToSpeak);
        if (audioFile == null || !audioFile.exists()) {
            model.addAttribute("error", "Не удалось создать аудиофайл.");
            populateModel(model);
            return "admin";
        }

        guild.getAudioManager().openAudioConnection(channel);

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        playerManager.loadItem(audioFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            guild.getAudioManager().closeAudioConnection();
                        }
                    }
                });
            }

            @Override public void playlistLoaded(AudioPlaylist playlist) {}
            @Override public void noMatches() { log.warn("Файл не найден"); }
            @Override public void loadFailed(FriendlyException exception) { log.error("Ошибка загрузки", exception); }
        });

        model.addAttribute("message", "Сообщение озвучено.");
        populateModel(model);
        return "admin";
    }


    private File synthesizeWithSapiTTS(String text) {
        try {
            File outputFile = File.createTempFile("tts_audio", ".wav");

            String escapedText = text.replace("'", "''");

            String command = String.format(
                    "powershell -Command \"$synth = New-Object -ComObject SAPI.SpVoice; " +
                            "$voice = $synth.GetVoices() | Where-Object { $_.GetDescription() -like '*%s*' }; " +
                            "$synth.Voice = $voice.Item(0); " +
                            "$file = '%s'; " +
                            "$audio = New-Object -ComObject SAPI.SpFileStream; " +
                            "$audio.Open($file, 3, $null); " +
                            "$synth.AudioOutputStream = $audio; " +
                            "$synth.Speak('%s'); " +
                            "$audio.Close()\"",
                    "Microsoft David",
                    outputFile.getAbsolutePath().replace("\\", "\\\\"),
                    escapedText
            );

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            return outputFile;
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при синтезе речи", e);
            return null;
        }
    }


}
