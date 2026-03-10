package com.evalai.backend.dto;

public record PythonEvalRequest(
        String question_number,
        String student_text,
        String teacher_facts,
        String teacher_diagram_rubric,
        Boolean contains_math,
        Boolean has_diagram,
        String diagram_snippet
) {}