package com.evalai.backend.dto;

public record PythonOcrResponse(
                String status,
                String document_type,
                String structured_data) {
}