package com.evalai.backend.dto;

import java.util.List;

public record PythonIngestResponse(
        List<Double> vector_embedding,
        List<TripletDTO> triplets
) {}
