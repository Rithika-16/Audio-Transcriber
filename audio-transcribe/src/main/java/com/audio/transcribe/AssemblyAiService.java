package com.audio.transcribe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class AssemblyAiService {

    @Value("${assemblyai.api.key}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public String transcribe(MultipartFile file) throws IOException, InterruptedException, ParseException {
        String uploadUrl = uploadFileToAssembly(file);
        String transcriptId = requestTranscription(uploadUrl);
        return pollTranscription(transcriptId);
    }

    private String uploadFileToAssembly(MultipartFile file) throws IOException, ParseException {
        String uploadEndpoint = "https://api.assemblyai.com/v2/upload";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadRequest = new HttpPost(uploadEndpoint);
            uploadRequest.addHeader("authorization", apiKey);

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file.getBytes(), org.apache.hc.core5.http.ContentType.DEFAULT_BINARY, file.getOriginalFilename())
                    .build();

            uploadRequest.setEntity(entity);

            String responseJson = EntityUtils.toString(httpClient.execute(uploadRequest).getEntity());
            JsonNode node = mapper.readTree(responseJson);
            return node.get("upload_url").asText();
        }
    }

    private String requestTranscription(String audioUrl) throws IOException, ParseException {
        String transcriptionEndpoint = "https://api.assemblyai.com/v2/transcript";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(transcriptionEndpoint);
            request.addHeader("authorization", apiKey);
            request.addHeader("content-type", "application/json");

            String json = String.format("{\"audio_url\":\"%s\"}", audioUrl);
            request.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity(json));

            String responseJson = EntityUtils.toString(httpClient.execute(request).getEntity());
            JsonNode node = mapper.readTree(responseJson);
            return node.get("id").asText();
        }
    }

    private String pollTranscription(String id) throws IOException, InterruptedException, ParseException {
        String statusEndpoint = "https://api.assemblyai.com/v2/transcript/" + id;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            while (true) {
                HttpGet request = new HttpGet(statusEndpoint);
                request.addHeader("authorization", apiKey);

                String responseJson = EntityUtils.toString(httpClient.execute(request).getEntity());
                JsonNode node = mapper.readTree(responseJson);
                String status = node.get("status").asText();

                if ("completed".equals(status)) {
                    return node.get("text").asText();
                } else if ("error".equals(status)) {
                    throw new RuntimeException("Transcription failed: " + node.get("error").asText());
                }

                TimeUnit.SECONDS.sleep(3); // wait and retry
            }
        }
    }
}