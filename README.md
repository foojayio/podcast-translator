# Podcast Translator Project

This guide explains how to set up and run the fully open-source, locally-run MediaTranslator application which requires
no cloud services.

## Prerequisites

### Required Software

- Java JDK 21 or higher
- Maven for dependency management
- FFmpeg (for audio extraction and processing)
- Ollama (for LLM capabilities) - https://ollama.com

### Speech-to-Text Option:

- OpenAI Whisper locally installed:
    - Install `whisper.cpp`:
        - Clone from https://github.com/ggerganov/whisper.cpp
        - In the directory, run `make`
        - Download a model, e.g. `sh ./models/download-ggml-model.sh large-v3-turbo-q8_0`

### Choose One Text-to-Speech Option:

- eSpeak: `sudo apt-get install espeak` (Linux)
- Pico2Wave: `sudo apt-get install libttspico-utils` (Linux)
- Mac's built-in 'say' command (MacOS only)
- Python pyttsx3: `pip install pyttsx3`

## Setting up Ollama

1. Install Ollama from https://ollama.com
2. Pull a model for translation and text enhancement:
   ```
   ollama pull mistral    # Basic, fast model
   ollama pull llama2     # Alternative option
   ollama pull mixtral    # More capable for complex tasks
   ```
3. Ensure the Ollama service is running before using the application

## Building and Running the Application

```
mvn clean package
java -jar target/media-translator-1.0-SNAPSHOT.jar
```

## Usage Instructions

1. Create a settings file `settings.properties`, based on `src/main/resources/settings-example.properties`
2. Run the application
3. The application will process the file and generate the translated audio

## Troubleshooting

### Speech Recognition Issues

- Make sure either whisper.cpp is properly installed

### Text-to-Speech Issues

- The application will try multiple TTS options in order
- If using pyttsx3, ensure you have compatible voices installed for your target language

### Ollama Connection Issues

- Check that the Ollama service is running on http://localhost:11434
- Verify you have pulled the model you're trying to use

## Supported Languages

The application supports translation between any language that Ollama models can handle. Speech synthesis capabilities
may vary depending on your chosen TTS system, but commonly supported languages include:

- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Italian (it)
- Portuguese (pt)
- Russian (ru)
- Chinese (zh)
- Japanese (ja)

To add more language support for Text-to-Speech, install additional language packs for your chosen TTS system.