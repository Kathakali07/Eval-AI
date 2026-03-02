package com.evalai.backend.dto;

import java.util.List;

public record PythonEvalRequest(
        String student_text,
        List<Double> model_vector,
        List<TripletDTO> model_triplets
) {}