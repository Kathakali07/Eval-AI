package com.evalai.backend.dto;

import java.util.List;

public record PythonEvalResponse(
        double semantic_score,
        List<String> missing_concepts,
        String feedback
) {}
