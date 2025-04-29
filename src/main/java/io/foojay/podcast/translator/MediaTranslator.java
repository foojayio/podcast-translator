package io.foojay.podcast.translator;

import io.foojay.podcast.translator.helper.OllamaHelper;
import io.foojay.podcast.translator.helper.Settings;
import io.foojay.podcast.translator.helper.WhisperHelper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * MediaTranslator - A Java application that takes MP3/MP4 files,
 * transcribes them, translates the text, and creates new audio files
 * in the target language using fully local and open source tools.
 */
public class MediaTranslator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Media Translation Application ===");

        var settings = new Settings();

        try {
            // Check if Ollama is running
            if (!OllamaHelper.isOllamaRunning()) {
                System.out.println("Error: Ollama service is not running. Please start Ollama first.");
                return;
            }

            // Original audio to transcription
            boolean transcriptionSucceeded = WhisperHelper.transcribeAudio(settings);
            if (!transcriptionSucceeded) {
                System.err.println("Error: Transcription failed. Please check the log for details.");
                return;
            }

            // Read the JSON output file
            String jsonContent = new String(Files.readAllBytes(Paths.get(settings.getOutputTranscriptFile())), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonContent);

            // Extract the transcribed text from the JSON
            String transcribedText = json.get("transcription").toString();

            //System.out.println("Transcription complete: " + transcribedText.substring(0, Math.min(100, transcribedText.length())) + "...");

            // Optional: Enhance transcription with Ollama
            String enhancedText = OllamaHelper.enhanceTranscriptionWithOllama(transcribedText, settings.getModel());
            System.out.println("Transcription enhanced!");

            // Translate text to target language using Ollama
            String translatedText = OllamaHelper.translateTextWithOllama(enhancedText, settings.getInputLanguage(), settings.getOutputLanguage(), settings.getModel());
            System.out.println("Translation complete: " + translatedText.substring(0, Math.min(100, translatedText.length())) + "...");

            // Convert translated text to speech using locally installed TTS
            generateSpeechWithLocalTTS(translatedText, settings.getOutputLanguage(), settings.getOutputAudioFile());
            System.out.println("Audio generation complete!");

            // Done
            System.out.println("Translation complete! Output saved to: " + settings.getOutputAudioFile());
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }


    private static void generateSpeechWithLocalTTS(String text, String language, String outputFilePath) throws IOException, InterruptedException {
        // Determine which TTS system to use based on what's available
        if (isCommandAvailable("espeak")) {
            generateSpeechWithEspeak(text, language, outputFilePath);
        } else if (isCommandAvailable("pico2wave")) {
            generateSpeechWithPico2Wave(text, language, outputFilePath);
        } else if (isCommandAvailable("say") && System.getProperty("os.name").toLowerCase().contains("mac")) {
            generateSpeechWithMacSay(text, language, outputFilePath);
        } else if (isCommandAvailable("python") && isPythonLibraryInstalled("pyttsx3")) {
            generateSpeechWithPyttsx3(text, language, outputFilePath);
        } else {
            throw new IOException("No supported TTS system found. Please install espeak, pico2wave, or pyttsx3.");
        }
    }

    private static void generateSpeechWithEspeak(String text, String language, String outputFilePath) throws IOException, InterruptedException {
        // Create a temporary text file for the input
        String tempTextPath = "temp_text_for_tts.txt";
        Files.write(Paths.get(tempTextPath), text.getBytes());

        // Convert language code to espeak format
        String espeakLanguage = convertToEspeakLanguage(language);

        // Use espeak to generate speech
        ProcessBuilder pb = new ProcessBuilder(
                "espeak", "-v", espeakLanguage, "-f", tempTextPath, "-w", outputFilePath
        );
        Process process = pb.start();
        int exitCode = process.waitFor();

        // Clean up temporary file
        Files.deleteIfExists(Paths.get(tempTextPath));

        if (exitCode != 0) {
            throw new IOException("espeak process failed with exit code " + exitCode);
        }
    }

    private static void generateSpeechWithPico2Wave(String text, String language, String outputFilePath) throws IOException, InterruptedException {
        // Convert language code to pico2wave format
        String picoLanguage = convertToPico2WaveLanguage(language);

        // Pico2wave has text length limitations, so we'll chunk the text
        List<String> textChunks = splitTextIntoChunks(text, 500);
        List<String> tempWavFiles = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            String tempWavPath = "temp_" + i + ".wav";
            tempWavFiles.add(tempWavPath);

            // Use pico2wave to generate speech for this chunk
            ProcessBuilder pb = new ProcessBuilder(
                    "pico2wave", "-l", picoLanguage, "-w", tempWavPath, textChunks.get(i)
            );
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // Clean up any created files
                for (String filePath : tempWavFiles) {
                    Files.deleteIfExists(Paths.get(filePath));
                }
                throw new IOException("pico2wave process failed with exit code " + exitCode);
            }
        }

        // Combine all wav files using ffmpeg
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");

        for (String wavFile : tempWavFiles) {
            command.add("-i");
            command.add(wavFile);
        }

        command.add("-filter_complex");

        StringBuilder filterComplex = new StringBuilder();
        for (int i = 0; i < tempWavFiles.size(); i++) {
            filterComplex.append("[").append(i).append(":0]");
        }
        filterComplex.append("concat=n=").append(tempWavFiles.size()).append(":v=0:a=1[out]");

        command.add(filterComplex.toString());
        command.add("-map");
        command.add("[out]");
        command.add(outputFilePath);

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        int exitCode = process.waitFor();

        // Clean up temporary files
        for (String filePath : tempWavFiles) {
            Files.deleteIfExists(Paths.get(filePath));
        }

        if (exitCode != 0) {
            throw new IOException("ffmpeg process failed with exit code " + exitCode);
        }
    }

    private static void generateSpeechWithMacSay(String text, String language, String outputFilePath) throws IOException, InterruptedException {
        // Convert language code to Mac voice
        String macVoice = convertToMacVoice(language);

        // Create a temporary aiff file
        String tempAiffPath = "temp_speech.aiff";

        // Use macOS 'say' command to generate speech
        ProcessBuilder pb = new ProcessBuilder(
                "say", "-v", macVoice, "-o", tempAiffPath, text
        );
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            Files.deleteIfExists(Paths.get(tempAiffPath));
            throw new IOException("say process failed with exit code " + exitCode);
        }

        // Convert aiff to mp3 using ffmpeg
        ProcessBuilder ffmpegPb = new ProcessBuilder(
                "ffmpeg", "-i", tempAiffPath, outputFilePath
        );
        Process ffmpegProcess = ffmpegPb.start();
        exitCode = ffmpegProcess.waitFor();

        // Clean up temporary file
        Files.deleteIfExists(Paths.get(tempAiffPath));

        if (exitCode != 0) {
            throw new IOException("ffmpeg process failed with exit code " + exitCode);
        }
    }

    private static void generateSpeechWithPyttsx3(String text, String language, String outputFilePath) throws IOException, InterruptedException {
        // Create a Python script to use pyttsx3
        String scriptPath = "tts_script.py";
        String scriptContent =
                "import sys\n" +
                        "import pyttsx3\n" +
                        "\n" +
                        "text = sys.argv[1]\n" +
                        "output_path = sys.argv[2]\n" +
                        "language = sys.argv[3]\n" +
                        "\n" +
                        "# Initialize the TTS engine\n" +
                        "engine = pyttsx3.init()\n" +
                        "\n" +
                        "# Try to set language/voice\n" +
                        "voices = engine.getProperty('voices')\n" +
                        "for voice in voices:\n" +
                        "    if language in voice.id.lower() or language in voice.name.lower():\n" +
                        "        engine.setProperty('voice', voice.id)\n" +
                        "        break\n" +
                        "\n" +
                        "# Set properties\n" +
                        "engine.setProperty('rate', 150)  # Speed\n" +
                        "engine.setProperty('volume', 0.9)  # Volume (0.0 to 1.0)\n" +
                        "\n" +
                        "# Save to file\n" +
                        "engine.save_to_file(text, output_path)\n" +
                        "engine.runAndWait()\n" +
                        "print(f'Speech saved to {output_path}')\n";

        Files.write(Paths.get(scriptPath), scriptContent.getBytes());

        // Run the Python script
        ProcessBuilder pb = new ProcessBuilder("python", scriptPath, text, outputFilePath, language);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Log output for debugging
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("pyttsx3: " + line);
        }

        int exitCode = process.waitFor();

        // Clean up script
        Files.deleteIfExists(Paths.get(scriptPath));

        if (exitCode != 0) {
            throw new IOException("Python pyttsx3 script failed with exit code " + exitCode);
        }
    }

    private static List<String> splitTextIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        int startPos = 0;

        while (startPos < text.length()) {
            int endPos = Math.min(startPos + maxChunkSize, text.length());

            // Try to end at a sentence or punctuation to sound more natural
            if (endPos < text.length()) {
                int sentenceEnd = findSentenceEnd(text, startPos, endPos);
                if (sentenceEnd > startPos) {
                    endPos = sentenceEnd;
                }
            }

            chunks.add(text.substring(startPos, endPos));
            startPos = endPos;
        }

        return chunks;
    }

    private static int findSentenceEnd(String text, int startPos, int maxEndPos) {
        int lastSentenceEnd = startPos;

        for (int i = startPos; i < maxEndPos; i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                lastSentenceEnd = i + 1;
            }
        }

        return lastSentenceEnd > startPos ? lastSentenceEnd : -1;
    }

    private static String convertToEspeakLanguage(String languageCode) {
        // Map ISO language codes to espeak voice names
        switch (languageCode.toLowerCase()) {
            case "en":
                return "en";
            case "fr":
                return "fr";
            case "es":
                return "es";
            case "de":
                return "de";
            case "it":
                return "it";
            case "pt":
                return "pt";
            case "ru":
                return "ru";
            case "zh":
                return "zh";
            case "ja":
                return "ja";
            default:
                return languageCode;
        }
    }

    private static String convertToPico2WaveLanguage(String languageCode) {
        // Map ISO language codes to pico2wave language codes
        switch (languageCode.toLowerCase()) {
            case "en":
                return "en-US";
            case "fr":
                return "fr-FR";
            case "es":
                return "es-ES";
            case "de":
                return "de-DE";
            case "it":
                return "it-IT";
            default:
                return "en-US"; // Default to English if not supported
        }
    }

    private static String convertToMacVoice(String languageCode) {
        // Map ISO language codes to macOS voice names
        switch (languageCode.toLowerCase()) {
            case "en":
                return "Alex";
            case "fr":
                return "Thomas";
            case "es":
                return "Juan";
            case "de":
                return "Anna";
            case "it":
                return "Alice";
            case "ja":
                return "Kyoko";
            case "zh":
                return "Tingting";
            default:
                return "Alex"; // Default to English if not supported
        }
    }

    private static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("where", command);
            } else {
                pb = new ProcessBuilder("which", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPythonLibraryInstalled(String library) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "-c", "import " + library);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getFileExtension(String filePath) {
        String fileName = new File(filePath).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}
