package com.evalai.backend.dto;

public record StudentAnswerItemDTO(
        String question_number,
        String student_text,
        Boolean contains_math,
        Boolean has_diagram,
        String diagram_snippet
) {}
