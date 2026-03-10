package com.evalai.backend.dto;

public record QAPairDTO(
        String question_number,
        String question_text,
        String model_answer,
        Double max_marks,
        Boolean contains_math,
        Boolean has_diagram,
        String diagram_snippet
) {}
