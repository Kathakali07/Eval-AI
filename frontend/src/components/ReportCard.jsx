import React from 'react';
import { ArrowLeft, AlertTriangle } from 'lucide-react';
import './ReportCard.css';

const ReportCard = ({ data, onGradeAnother }) => {
    const {
        subject,
        total_score_achieved: scored,
        total_max_marks: maxMarks,
        graded_questions: questions = [],
    } = data;

    const percentage = maxMarks > 0 ? (scored / maxMarks) * 100 : 0;

    // SVG circular progress
    const radius = 70;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (percentage / 100) * circumference;

    const getGradeColor = (pct) => {
        if (pct >= 80) return '#10b981'; // green
        if (pct >= 60) return '#3b82f6'; // blue
        if (pct >= 40) return '#f59e0b'; // amber
        return '#ef4444'; // red
    };

    const getGradeLabel = (pct) => {
        if (pct >= 90) return 'Excellent';
        if (pct >= 80) return 'Very Good';
        if (pct >= 70) return 'Good';
        if (pct >= 60) return 'Above Average';
        if (pct >= 50) return 'Average';
        if (pct >= 40) return 'Below Average';
        return 'Needs Improvement';
    };

    const gradeColor = getGradeColor(percentage);
    const gradeLabel = getGradeLabel(percentage);

    return (
        <div className="report-card animate-slide-up">

            {/* ── Back Button ── */}
            <button className="rc-back-btn" onClick={onGradeAnother}>
                <ArrowLeft size={18} />
                <span>Grade Another Exam</span>
            </button>

            {/* ── Header Card ── */}
            <div className="rc-header-card">
                <div className="rc-header-info">
                    <span className="rc-badge">AI Report Card</span>
                    <h1 className="rc-subject">{subject || 'Subject'}</h1>
                    <span className="rc-grade-label" style={{ color: gradeColor }}>{gradeLabel}</span>
                </div>

                <div className="rc-score-ring-container">
                    <svg className="rc-score-ring" viewBox="0 0 160 160">
                        {/* Background ring */}
                        <circle
                            cx="80" cy="80" r={radius}
                            fill="none"
                            stroke="rgba(0,0,0,0.06)"
                            strokeWidth="10"
                        />
                        {/* Progress ring */}
                        <circle
                            cx="80" cy="80" r={radius}
                            fill="none"
                            stroke={gradeColor}
                            strokeWidth="10"
                            strokeLinecap="round"
                            strokeDasharray={circumference}
                            strokeDashoffset={offset}
                            className="rc-progress-circle"
                            transform="rotate(-90 80 80)"
                        />
                    </svg>
                    <div className="rc-score-text">
                        <span className="rc-score-achieved" style={{ color: gradeColor }}>{scored}</span>
                        <span className="rc-score-divider">/</span>
                        <span className="rc-score-max">{maxMarks}</span>
                    </div>
                </div>
            </div>

            {/* ── Questions List ── */}
            <div className="rc-questions">
                <h2 className="rc-section-title">Question-Wise Breakdown</h2>

                {questions.map((q, idx) => {
                    const qPct = q.max_marks > 0 ? (q.score_achieved / q.max_marks) * 100 : 0;
                    const qColor = getGradeColor(qPct);
                    return (
                        <div className="rc-question-card" key={idx}>
                            <div className="rc-q-header">
                                <div className="rc-q-number">
                                    <span className="rc-q-label">Q{q.question_number}</span>
                                </div>
                                <div className="rc-q-score-info">
                                    <span className="rc-q-score" style={{ color: qColor }}>
                                        {q.score_achieved}
                                    </span>
                                    <span className="rc-q-of">/</span>
                                    <span className="rc-q-max">{q.max_marks}</span>
                                    <span className="rc-q-marks-label">marks</span>
                                </div>
                            </div>

                            {/* Score bar */}
                            <div className="rc-q-bar-track">
                                <div
                                    className="rc-q-bar-fill"
                                    style={{ width: `${qPct}%`, background: qColor }}
                                />
                            </div>

                            {/* Feedback */}
                            {q.feedback && (
                                <p className="rc-q-feedback">{q.feedback}</p>
                            )}

                            {/* Missing Concepts */}
                            {q.missing_concepts && q.missing_concepts.length > 0 && (
                                <div className="rc-q-missing">
                                    <span className="rc-missing-label">
                                        <AlertTriangle size={13} />
                                        Missing Concepts
                                    </span>
                                    <div className="rc-pills">
                                        {q.missing_concepts.map((concept, ci) => (
                                            <span className="rc-pill" key={ci}>{concept}</span>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {/* ── Footer Action ── */}
            <div className="rc-footer-actions">
                <button className="rc-btn-grade-another" onClick={onGradeAnother}>
                    <ArrowLeft size={16} />
                    Grade Another Exam
                </button>
            </div>
        </div>
    );
};

export default ReportCard;
