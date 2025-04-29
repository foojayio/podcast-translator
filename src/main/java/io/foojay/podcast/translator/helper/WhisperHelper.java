package io.foojay.podcast.translator.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WhisperHelper {

    public static boolean transcribeAudio(Settings settings) throws IOException, InterruptedException {
        // Create the initial prompt with context and word hints
        createInitialPromptFile(settings);

        // Run Whisper locally using whisper.cpp or whisper.cpp command line
        // Alternatively, one could use the OpenAI Whisper model locally through a wrapper
        List<String> command = new ArrayList<>();

        command.add(settings.getWhisperPath());
        command.add("--model");
        command.add(settings.getWhisperModel());  // Use base model for better performance
        command.add("--language");
        command.add(settings.getInputLanguage());
        command.add("--output-json");
        command.add("--output-file");
        command.add(settings.getOutputTranscriptFile().replace(".json", ""));
        // Add initial prompt
        command.add("--prompt");
        command.add(new String(Files.readAllBytes(Paths.get(settings.getOutputPromptFile())), StandardCharsets.UTF_8));
        // Add word timestamp tokens to help with word alignment
        //command.add("--word-timestamps");
        //command.add("true");
        command.add(settings.getInputAudioFile());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Log output for debugging
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Whisper: " + line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // Fallback to using a Python script with Whisper if direct command failed
            System.err.println("Whisper ailed, error: " + exitCode);
            return false;
        }

        System.out.println("Whisper finished");
        return true;
    }

    private static void createInitialPromptFile(Settings settings) throws IOException {
        // Create a context-rich prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("This is a tech podcast about Java and software development. ");
        prompt.append("The podcast is called Foojay Podcast.");
        prompt.append("It frequently mentions people and technologies including: ");
        prompt.append(settings.getWordHints());
        prompt.append(". ");
        // Add any specific context about the current episode if available
        String episodeContext = settings.getEpisodeContext();
        if (!episodeContext.isEmpty()) {
            prompt.append(episodeContext);
        }

        Files.write(Paths.get(settings.getOutputPromptFile()), prompt.toString().getBytes(StandardCharsets.UTF_8));
    }

}
