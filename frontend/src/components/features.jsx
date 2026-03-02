import React from 'react';
import './features.css';

const featuresData = [
    {
        icon: '🤖',
        title: 'AI Descriptive Evaluation',
        description: 'Advanced NLP models evaluate long-form subjective answers contextually, prioritizing understanding over exact keyword matches.',
    },
    {
        icon: '📊',
        title: 'Extensive Actionable Feedback',
        description: 'Provides line-by-line feedback, highlighting areas of improvement, structural flaws, and conceptual gaps in answers.',
    },
    {
        icon: '⚡',
        title: 'Instant Auto-Grading',
        description: 'Scale your assessment process instantly. Grade thousands of descriptive assignments accurately in minutes.',
    },
    {
        icon: '🔍',
        title: 'Integrity & Verification',
        description: 'Ensures original thought by detecting AI-generated content and identifying potential plagiarism sources.',
    },
    {
        icon: '📈',
        title: 'Analytics Dashboard',
        description: 'Track overall class performance, identify common knowledge gaps, and download comprehensive assessment reports.',
    },
    {
        icon: '⚙️',
        title: 'Custom Evaluation Criteria',
        description: 'Define your own marking rubrics, setting specific weights for grammar, technical accuracy, and conceptual depth.',
    }
];

const Features = () => {
    return (
        <section id="features" className="section features">
            <div className="container">
                <div className="section-header text-center animate-slide-up">
                    <h2 className="section-title">
                        <span className="text-gradient">Why Choose</span> Eval AI
                    </h2>
                    <p className="section-subtitle">
                        A complete toolkit designed to revolutionize subjective assessments for educators, universities, and corporate trainers.
                    </p>
                </div>

                <div className="features-grid">
                    {featuresData.map((feature, index) => (
                        <div
                            className="glass-card feature-card animate-slide-up"
                            key={index}
                            style={{ animationDelay: `${index * 0.1}s` }}
                        >
                            <div className="feature-icon-wrapper">
                                <span className="feature-icon">{feature.icon}</span>
                                <div className="icon-glow"></div>
                            </div>
                            <h3 className="feature-title">{feature.title}</h3>
                            <p className="feature-desc">{feature.description}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default Features;
