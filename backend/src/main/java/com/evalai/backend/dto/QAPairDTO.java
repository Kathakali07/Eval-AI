package com.evalai.backend.dto;

// Maps a single question from the Teacher's Model Paper
public record QAPairDTO(
        String question_number,
        String question_text,
        String model_answer,
        Double max_marks
) {}
