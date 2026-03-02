import React from 'react';
import evalRobot from './eval-ai-robot.mp4';
import './Description.css';

const Description = () => {
    return (
        <section className="Desc">
            <div className="Desc-background">
                <div className="glow-orb orb-1"></div>
                <div className="glow-orb orb-2"></div>
            </div>

            <div className="container Desc-container">
                <div className="Desc-content">
                    <div className="badge animate-slide-up" style={{ animationDelay: '0.1s' }}>
                        <span className="badge-dot"></span>
                        Now Evaluating with GPT-4 Omni
                    </div>

                    <h1 className="Desc-title animate-slide-up" style={{ animationDelay: '0.2s' }}>
                        The Future of<br />
                        <span className="text-gradient-accent">Assessment</span> <br />
                        is Here
                    </h1>

                    <p className="Desc-subtitle animate-slide-up" style={{ animationDelay: '0.3s' }}>
                        Transform subjective assessments with AI-powered precision. Get instant descriptive feedback, automated grading and actionable insights to close the learning gap in seconds.
                    </p>

                    <div className="Desc-actions animate-slide-up" style={{ animationDelay: '0.4s' }}>
                        <a href="/try" className="btn btn-primary btn-large">Try it for Free</a>
                        <a href="#demo" className="btn btn-secondary btn-large">Watch Demo</a>
                    </div>

                    <div className="Desc-stats animate-slide-up" style={{ animationDelay: '0.5s' }}>
                        <div className="stat">
                            <span className="stat-value">99%</span>
                            <span className="stat-label">Accuracy</span>
                        </div>
                        <div className="stat-divider"></div>
                        <div className="stat">
                            <span className="stat-value">10x</span>
                            <span className="stat-label">Faster Grading</span>
                        </div>
                        <div className="stat-divider"></div>
                        <div className="stat">
                            <span className="stat-value">24/7</span>
                            <span className="stat-label">Feedback</span>
                        </div>
                    </div>
                </div>

                <div className="Desc-visual animate-slide-up" style={{ animationDelay: '0.4s' }}>
                    <div className="glass-panel animate-float">
                        <div className="panel-header">
                            <div className="mac-dots">
                                <span></span><span></span><span></span>
                            </div>
                            <div className="panel-title"></div>
                        </div>
                        <div className="panel-body video-body">
                            <video
                                autoPlay
                                loop
                                muted
                                playsInline
                                className="Desc-video"
                            >
                                <source src={evalRobot} type="video/mp4" />
                                Your browser does not support the video tag.
                            </video>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default Description;
