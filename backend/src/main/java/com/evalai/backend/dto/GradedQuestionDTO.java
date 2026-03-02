package com.evalai.backend.dto;

import java.util.List;

public record GradedQuestionDTO(
        String question_number,
        double score_achieved,
        double max_marks,
        List<String> missing_concepts,
        String feedback
) {}
