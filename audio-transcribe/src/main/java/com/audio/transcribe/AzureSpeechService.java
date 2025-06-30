package com.audio.transcribe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

@Service
public class AzureSpeechService {

    private final String subscriptionKey = "1DhiHQ65YWWW7K3IOS3oF4tsVymFuC6g3WmUIidRg19VcGGBTHmOJQQJ99BFACYeBjFXJ3w3AAAYACOGqBss";
    private final String region = "eastus";

    public String transcribeAudio(MultipartFile file, String language) throws IOException, InterruptedException {
        
        String endpoint = String.format(
                "https://%s.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=%s",
                region, language.trim()
        );

        
        File inputFile = File.createTempFile("input-", getExtension(file.getOriginalFilename()));
        File wavFile = File.createTempFile("converted-", ".wav");

        try {
            file.transferTo(inputFile);

            
            if (!file.getOriginalFilename().endsWith(".wav")) {
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg", "-y",
                        "-i", inputFile.getAbsolutePath(),
                        "-ar", "16000", 
                        "-ac", "1",     
                        wavFile.getAbsolutePath()
                );
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg conversion failed.");
                }
            } else {
                wavFile = inputFile; 
            }

        
            byte[] audioBytes = Files.readAllBytes(wavFile.toPath());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Content-Type", "audio/wav")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Azure Response Status: " + response.statusCode());
            System.out.println("Azure Response Body: " + response.body());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            return json.path("DisplayText").asText("No transcription found.");
        } finally {
            inputFile.delete();
            if (!wavFile.equals(inputFile)) {
                wavFile.delete();
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".wav";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
