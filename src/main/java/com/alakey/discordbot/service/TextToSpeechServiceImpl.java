package com.alakey.discordbot.service;

import com.tetradotoxina.gtts4j.GTTS4J;
import com.tetradotoxina.gtts4j.exception.GTTS4JException;
import com.tetradotoxina.gtts4j.impl.GTTS4JImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TextToSpeechServiceImpl implements TextToSpeechService {

    @Override
    public File synthesize(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст не может быть пустым");
        }

        try {
            GTTS4J gtts4j = new GTTS4JImpl();
            String lang = "ru";
            boolean slow = false;

            List<String> chunks = splitTextIntoChunks(text, 100);

            File mergedFile = File.createTempFile("tts_full_audio", ".mp3");
            try (var outputStream = new java.io.FileOutputStream(mergedFile)) {
                for (String chunk : chunks) {
                    byte[] data = gtts4j.textToSpeech(chunk, lang, slow);
                    outputStream.write(data);
                }
            }

            return mergedFile;
        } catch (IOException | GTTS4JException e) {
            throw new RuntimeException("Ошибка при синтезе речи через gtts4j", e);
        }
    }

    private List<String> splitTextIntoChunks(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentChunk = new StringBuilder();

        for (String word : words) {
            if (currentChunk.length() + word.length() + 1 > maxLength) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(word).append(" ");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
