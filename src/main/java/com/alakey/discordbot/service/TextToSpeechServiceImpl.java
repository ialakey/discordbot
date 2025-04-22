package com.alakey.discordbot.service;

import com.tetradotoxina.gtts4j.GTTS4J;
import com.tetradotoxina.gtts4j.exception.GTTS4JException;
import com.tetradotoxina.gtts4j.impl.GTTS4JImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class TextToSpeechServiceImpl implements TextToSpeechService {

    @Override
    public File synthesize(String text) {
        try {
            GTTS4J gtts4j = new GTTS4JImpl();
            String lang = "ru";
            boolean slow = false;

            byte[] data = gtts4j.textToSpeech(text, lang, slow);

            File tempMp3 = File.createTempFile("tts_audio", ".mp3");
            gtts4j.saveFile(tempMp3.getAbsolutePath(), data, true);

            return tempMp3;
        } catch (IOException | GTTS4JException e) {
            throw new RuntimeException("Ошибка при синтезе речи через gtts4j", e);
        }
    }
}
