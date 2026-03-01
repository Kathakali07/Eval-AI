import React from 'react';
import './how-it-works.css';

const steps = [
    {
        number: '01',
        title: 'Upload Responses',
        description: 'Instructors or students upload subjective answers in bulk via text or document along with model answer script.',
        highlight: 'Supports various formats'
    },
    {
        number: '02',
        title: 'Define Criteria',
        description: 'Set custom parameters, marking schemas and weightages for the AI to base its evaluation upon.',
        highlight: 'Fully customizable rubrics'
    },
    {
        number: '03',
        title: 'AI Analysis',
        description: 'Our proprietary models analyze semantic meaning, structure and original thought against your criteria.',
        highlight: 'Powered by GPT-4 Omni'
    },
    {
        number: '04',
        title: 'Review & Grade',
        description: 'Receive detailed grading, actionable feedback and a comprehensive breakdown of the assessment instantly.',
        highlight: 'Exportable reports'
    }
];

const HowItWorks = () => {
    return (
        <section id="how-it-works" className="section how-it-works">
            <div className="container">
                <div className="section-header text-center animate-slide-up">
                    <h2 className="section-title">
                        How It <span className="text-gradient">Works</span>
                    </h2>
                    <p className="section-subtitle">
                        Seamlessly integrate advanced AI evaluation into your existing workflow in four simple steps.
                    </p>
                </div>

                <div className="steps-container">
                    {/* Decorative connector line */}
                    <div className="steps-connector"></div>

                    {steps.map((step, index) => (
                        <div
                            className={`step-item animate-slide-up`}
                            key={index}
                            style={{ animationDelay: `${index * 0.15}s` }}
                        >
                            <div className="step-number-container">
                                <div className="step-number">{step.number}</div>
                                <div className="step-glow"></div>
                            </div>

                            <div className="step-content">
                                <div className="step-highlight">{step.highlight}</div>
                                <h3 className="step-title">{step.title}</h3>
                                <p className="step-desc">{step.description}</p>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default HowItWorks;
