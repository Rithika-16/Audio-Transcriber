package com.audio.transcribe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") // 👈 Important for React frontend
public class AudioTranscriberController {

    @Autowired
    private AzureSpeechService azureSpeechService;

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("language") String language) {
        try {
            String transcription = azureSpeechService.transcribeAudio(file, language);
            return ResponseEntity.ok(transcription);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    new ErrorResponse("Transcription failed", e.getMessage())
            );
        }
    }

    // Simple error response object
    static class ErrorResponse {
        public String error;
        public String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}