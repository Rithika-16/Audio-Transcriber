package com.audio.transcribe;

import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/transcribe")
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from frontend
public class TranscriptionController {

    @Autowired
    private AssemblyAiService assemblyAiService;

    @PostMapping
    public ResponseEntity<String> transcribeAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded.");
        }

        try {
            String transcription = assemblyAiService.transcribe(file);
            return ResponseEntity.ok(transcription);
        } catch (IOException | InterruptedException | ParseException e) {
            e.printStackTrace(); // Optional: use logger in real apps
            return ResponseEntity.status(500).body("Transcription failed: " + e.getMessage());
        }
    }
}