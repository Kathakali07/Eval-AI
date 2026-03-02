package com.evalai.backend.dto;

import java.util.List;

public record ReportCardDTO(
        String subject,
        double total_score_achieved,
        double total_max_marks,
        List<GradedQuestionDTO> graded_questions
) {}