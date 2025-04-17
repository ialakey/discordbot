package com.alakey.discordbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class TextToSpeechServiceImpl implements TextToSpeechService {

    private static final String VOICE_NAME = "Microsoft David";

    @Override
    public File synthesize(String text) {
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
                VOICE_NAME,
                outputFile.getAbsolutePath().replace("\\", "\\\\"),
                escapedText
            );

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            return outputFile;

        } catch (InterruptedException | IOException e) {
            log.error("Ошибка при синтезе речи", e);
            return null;
        }
    }
}
