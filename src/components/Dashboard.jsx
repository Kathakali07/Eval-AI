import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileText, CheckCircle, AlertCircle, Loader, LogOut, X, User, Clock, Settings, ChevronDown } from 'lucide-react';
import Footer from './footer';
import logo from './Eval-AI-Logo-only.png';
import './Dashboard.css';

const Dashboard = () => {
    const navigate = useNavigate();
    const [answerScript, setAnswerScript] = useState(null);
    const [modelAnswer, setModelAnswer] = useState(null);
    const [isAnalyzing, setIsAnalyzing] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const dropdownRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleFileUpload = (e, type) => {
        const file = e.target.files[0];
        if (!file) return;
        if (file.type !== 'application/pdf') {
            setError('Only PDF files are allowed.');
            return;
        }
        setError(null);
        if (type === 'answer') setAnswerScript(file);
        if (type === 'model') setModelAnswer(file);
    };

    const handleRemoveFile = (type) => {
        if (type === 'answer') setAnswerScript(null);
        if (type === 'model') setModelAnswer(null);
    };

    const handleAnalyze = async () => {
        if (!answerScript || !modelAnswer) {
            setError('Please upload both files before analyzing.');
            return;
        }
        setError(null);
        setIsAnalyzing(true);
        setResult(null);

        // ─────────────────────────────────────────
        // TODO: Backend API call goes here
        // e.g. const formData = new FormData();
        //      formData.append('answerScript', answerScript);
        //      formData.append('modelAnswer', modelAnswer);
        //      const res = await axios.post('/api/analyze', formData);
        //      setResult(res.data);
        // ─────────────────────────────────────────

        // Simulated result for frontend demo
        setTimeout(() => {
            setResult({
                score: '78/100',
                grade: 'B+',
                summary: 'The answer script demonstrates a good understanding of the core concepts. Key points were addressed with reasonable depth and clarity.',
                strengths: [
                    'Clear introduction and conclusion',
                    'Good use of relevant examples',
                    'Logical flow of arguments',
                ],
                improvements: [
                    'Could elaborate more on theoretical aspects',
                    'Some points lack supporting evidence',
                    'Minor grammatical inconsistencies',
                ],
                feedback: 'Overall a commendable attempt. The student shows a solid grasp of the subject matter. With more attention to detail and deeper analysis, this could easily achieve a higher grade.',
            });
            setIsAnalyzing(false);
        }, 2000);
    };

    const handleLogout = () => {
        // ─────────────────────────────────────────
        // TODO: Backend logout API call goes here
        // ─────────────────────────────────────────
        navigate('/login');
    };

    const handleReset = () => {
        setAnswerScript(null);
        setModelAnswer(null);
        setResult(null);
        setError(null);
    };

    return (
        <div className="dashboard-page">

            <div className="dashboard-bg">
                <div className="dash-orb orb-1"></div>
                <div className="dash-orb orb-2"></div>
            </div>

            <nav className="dash-navbar">
                <div className="dash-nav-inner">
                    <a href="/" className="logo">
                        <span className="dash-icon">
                            <img src={logo} alt="Eval AI Logo" />
                        </span>
                        <span className="dash-logo">Eval <span className="text-gradient-accent">AI</span></span>
                    </a>

                    <div className="profile-menu" ref={dropdownRef}>
                        <button className="profile-trigger" onClick={() => setDropdownOpen(!dropdownOpen)}>
                            <div className="profile-avatar">
                                <User size={18} />
                            </div>
                            <span className="profile-name">My Account</span>
                            <ChevronDown size={14} className={`chevron ${dropdownOpen ? 'open' : ''}`} />
                        </button>

                        {dropdownOpen && (
                            <div className="profile-dropdown">
                                <div className="dropdown-header">
                                    <div className="dropdown-avatar"><User size={20} /></div>
                                    <div className="dropdown-user-info">
                                        <span className="dropdown-username">Username</span>
                                        <span className="dropdown-email">user@email.com</span>
                                    </div>
                                </div>

                                <div className="dropdown-divider"></div>

                                <button className="dropdown-item" onClick={() => { setDropdownOpen(false); /* navigate('/profile') */ }}>
                                    <User size={15} />
                                    Profile
                                </button>
                                <button className="dropdown-item" onClick={() => { setDropdownOpen(false); /* navigate('/history') */ }}>
                                    <Clock size={15} />
                                    History
                                </button>
                                <button className="dropdown-item" onClick={() => { setDropdownOpen(false); /* navigate('/settings') */ }}>
                                    <Settings size={15} />
                                    Settings
                                </button>

                                <div className="dropdown-divider"></div>

                                <button className="dropdown-item logout-item" onClick={handleLogout}>
                                    <LogOut size={15} />
                                    Logout
                                </button>
                            </div>
                        )}
                    </div>

                </div>
            </nav>

            <main className="dash-main">
                <div className="dash-header animate-slide-up">
                    <a href="/" className="logo">
                    <span className="logo-icon">
                        <img src={logo} alt="Eval AI Logo" />
                    </span>
                </a>
                    <p className="dash-subtitle">Upload your answer script and model answer to get instant AI-powered feedback</p>
                </div>

                <div className="dash-workspace">

                    <div className="dash-panel upload-panel animate-slide-up">
                        <div className="panel-label">
                            <span className="step-highlight">Step 01</span>
                            <h2>Upload Files</h2>
                            <p>Upload the answer script and model answer in PDF format</p>
                        </div>

                        <div className="upload-zone">
                            <div className="upload-zone-header">
                                <FileText size={18} />
                                <span>Answer Script</span>
                            </div>
                            {answerScript ? (
                                <div className="file-preview">
                                    <div className="file-info">
                                        <CheckCircle size={16} className="file-check" />
                                        <span className="file-name">{answerScript.name}</span>
                                    </div>
                                    <button className="remove-file" onClick={() => handleRemoveFile('answer')}><X size={14} /></button>
                                </div>
                            ) : (
                                <label className="upload-drop-area" htmlFor="answer-upload">
                                    <Upload size={28} className="upload-icon" />
                                    <span className="upload-text">Click to upload PDF</span>
                                    <span className="upload-hint">PDF files only</span>
                                    <input id="answer-upload" type="file" accept=".pdf" onChange={(e) => handleFileUpload(e, 'answer')} hidden />
                                </label>
                            )}
                        </div>

                        <div className="upload-zone">
                            <div className="upload-zone-header">
                                <FileText size={18} />
                                <span>Model Answer</span>
                            </div>
                            {modelAnswer ? (
                                <div className="file-preview">
                                    <div className="file-info">
                                        <CheckCircle size={16} className="file-check" />
                                        <span className="file-name">{modelAnswer.name}</span>
                                    </div>
                                    <button className="remove-file" onClick={() => handleRemoveFile('model')}><X size={14} /></button>
                                </div>
                            ) : (
                                <label className="upload-drop-area" htmlFor="model-upload">
                                    <Upload size={28} className="upload-icon" />
                                    <span className="upload-text">Click to upload PDF</span>
                                    <span className="upload-hint">PDF files only</span>
                                    <input id="model-upload" type="file" accept=".pdf" onChange={(e) => handleFileUpload(e, 'model')} hidden />
                                </label>
                            )}
                        </div>

                        {error && (
                            <div className="dash-error">
                                <AlertCircle size={16} />
                                {error}
                            </div>
                        )}

                        <div className="upload-actions">
                            <button className="btn-analyze" onClick={handleAnalyze} disabled={isAnalyzing}>
                                {isAnalyzing ? <><Loader size={18} className="spin" /> Analyzing...</> : 'Analyze Now'}
                            </button>
                            <button className="btn-reset" onClick={handleReset}>Reset</button>
                        </div>
                    </div>

                    <div className="dash-panel result-panel animate-slide-up">
                        <div className="panel-label">
                            <span className="step-highlight">Step 02</span>
                            <h2>Analysis Result</h2>
                            <p>AI-generated evaluation will appear here</p>
                        </div>

                        {!result && !isAnalyzing && (
                            <div className="result-empty">
                                <div className="empty-icon">📄</div>
                                <p>Upload both files and click <strong>Analyze Now</strong> to see results</p>
                            </div>
                        )}

                        {isAnalyzing && (
                            <div className="result-loading">
                                <Loader size={36} className="spin" />
                                <p>Analyzing your answer script...</p>
                            </div>
                        )}

                        {result && (
                            <div className="result-content">

                                <div className="result-score-row">
                                    <div className="score-box">
                                        <span className="score-value">{result.score}</span>
                                        <span className="score-label">Score</span>
                                    </div>
                                    <div className="score-box">
                                        <span className="score-value">{result.grade}</span>
                                        <span className="score-label">Grade</span>
                                    </div>
                                </div>

                                <div className="result-section">
                                    <h4 className="result-section-title">Summary</h4>
                                    <p className="result-text">{result.summary}</p>
                                </div>

                                <div className="result-section">
                                    <h4 className="result-section-title strengths">✓ Strengths</h4>
                                    <ul className="result-list">
                                        {result.strengths.map((s, i) => <li key={i}>{s}</li>)}
                                    </ul>
                                </div>

                                <div className="result-section">
                                    <h4 className="result-section-title improvements">⚠ Areas to Improve</h4>
                                    <ul className="result-list improvements-list">
                                        {result.improvements.map((s, i) => <li key={i}>{s}</li>)}
                                    </ul>
                                </div>

                                <div className="result-section feedback-box">
                                    <h4 className="result-section-title">💬 Overall Feedback</h4>
                                    <p className="result-text">{result.feedback}</p>
                                </div>

                            </div>
                        )}
                    </div>

                </div>
            </main>
            <Footer />
        </div>
    );
};

export default Dashboard;