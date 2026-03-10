package com.evalai.backend.dto;

public record PythonOcrResponse(
                String status,
                String document_type,
                Double confidence_score,
                String structured_data) {
}