package io.foojay.podcast.translator.helper;

import io.foojay.podcast.translator.MediaTranslator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    private static final Properties settings = new Properties();

    public Settings() {
        // Try loading from resources first
        try (InputStream resourceStream = MediaTranslator.class.getResourceAsStream("/settings.properties")) {
            if (resourceStream != null) {
                settings.load(resourceStream);
                return;
            }
        } catch (IOException e) {
            System.err.println("Could not load settings from resources: " + e.getMessage());
        }

        // If not found in resources, try loading from executable location
        try {
            String execPath = new File(MediaTranslator.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile()
                    .getPath();
            File settingsFile = new File(execPath, "settings.properties");

            if (settingsFile.exists()) {
                try (FileInputStream fis = new FileInputStream(settingsFile)) {
                    settings.load(fis);
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load settings from executable location: " + e.getMessage());
        }

        throw new RuntimeException("Could not find settings.properties in resources or executable location");
    }

    // Replace the constants with property getters
    public String getInputAudioFile() {
        return settings.getProperty("input.audio.file");
    }

    public String getOutputPromptFile() {
        return settings.getProperty("output.prompt.file");
    }

    public String getOutputTranscriptFile() {
        return settings.getProperty("output.transcript.file");
    }

    public String getOutputAudioFile() {
        return settings.getProperty("output.audio.file");
    }

    public String getInputLanguage() {
        return settings.getProperty("input.language");
    }

    public String getOutputLanguage() {
        return settings.getProperty("output.language");
    }

    public String getModel() {
        return settings.getProperty("model");
    }

    public String getWhisperPath() {
        return settings.getProperty("whisper.path");
    }

    public String getWhisperModel() {
        return settings.getProperty("whisper.model");
    }

    public String getWordHints() {
        return settings.getProperty("word.hints");
    }

    public String getEpisodeContext() {
        return settings.getProperty("episode.context");
    }
}
