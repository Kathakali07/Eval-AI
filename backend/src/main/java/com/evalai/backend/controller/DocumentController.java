package com.evalai.backend.controller;



import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins="*")
public class DocumentController {


    private final RestClient restClient;

    public DocumentController() {
        this.restClient = RestClient.builder().baseUrl("127.0.0.1:8000").build();

    }

    @PostMapping(value = "/upload-exam", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> processStudentExam(@RequestParam MultipartFile file){
        try{

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            System.out.println("Sending exam data to Ai-Engine");

            String response = restClient.post()
                    .uri("/extract-text")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            System.out.println("Recieved text from Ai-engine");
            return ResponseEntity.ok(response);

        }catch(Exception e){
            System.err.println("Failed to reach Ai-Engine "+e.getMessage());
            return ResponseEntity.internalServerError().body("Error communication to Ai-Engine");
        }
    }
}
