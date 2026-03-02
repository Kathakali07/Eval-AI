import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Upload, CheckCircle, AlertCircle, Loader, LogOut,
    X, User, ChevronDown, BookOpen, GraduationCap, Sparkles, Zap
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Footer from './footer';
import ReportCard from './ReportCard';
import logo from './Eval-AI-Logo-only.png';
import './Dashboard.css';

const API_BASE = 'http://localhost:8080';

const Dashboard = () => {
    const { token, user, logout } = useAuth();

    // Form state
    const [subjectArea, setSubjectArea] = useState('');
    const [teacherFile, setTeacherFile] = useState(null);
    const [studentFile, setStudentFile] = useState(null);

    // Processing state
    const [isProcessing, setIsProcessing] = useState(false);
    const [processingStep, setProcessingStep] = useState(0);
    const [reportCard, setReportCard] = useState(null);

    // UI state
    const [error, setError] = useState(null);
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const [draggingZone, setDraggingZone] = useState(null);
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

    useEffect(() => {
        if (error) {
            const timer = setTimeout(() => setError(null), 6000);
            return () => clearTimeout(timer);
        }
    }, [error]);

    const ALLOWED_TYPES = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg', 'image/webp'];

    const validateFile = (file) => {
        if (!file) return false;
        if (!ALLOWED_TYPES.includes(file.type)) {
            setError('Only PDF and image files (PNG, JPG, WebP) are allowed.');
            return false;
        }
        setError(null);
        return true;
    };

    const handleFileSelect = (e, zone) => {
        const file = e.target.files[0];
        if (!validateFile(file)) return;
        if (zone === 'teacher') setTeacherFile(file);
        else setStudentFile(file);
    };

    const handleDrop = (e, zone) => {
        e.preventDefault();
        e.stopPropagation();
        setDraggingZone(null);
        const file = e.dataTransfer.files[0];
        if (!validateFile(file)) return;
        if (zone === 'teacher') setTeacherFile(file);
        else setStudentFile(file);
    };

    const handleDragOver = (e, zone) => {
        e.preventDefault();
        e.stopPropagation();
        setDraggingZone(zone);
    };

    const handleDragLeave = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDraggingZone(null);
    };

    const isReady = subjectArea.trim() && teacherFile && studentFile;

    /** Authenticated fetch helper — injects Bearer token. */
    const authFetch = (url, options = {}) => {
        return fetch(url, {
            ...options,
            headers: {
                ...options.headers,
                'Authorization': `Bearer ${token}`,
            },
        });
    };

    /** Two-step sequential API chain: ingest model paper, then grade student paper. */
    const handleProcessAndGrade = async () => {
        if (!isReady) return;
        setError(null);
        setIsProcessing(true);

        try {
            // Step 1: Ingest model paper
            setProcessingStep(1);
            const ingestForm = new FormData();
            ingestForm.append('subjectArea', subjectArea.trim());
            ingestForm.append('file', teacherFile);

            const ingestRes = await authFetch(`${API_BASE}/api/ingestion/upload-teacher-paper`, {
                method: 'POST',
                body: ingestForm,
            });

            if (!ingestRes.ok) {
                const errText = await ingestRes.text();
                throw new Error(errText || `Ingestion failed (HTTP ${ingestRes.status})`);
            }

            // Step 2: Grade student paper
            setProcessingStep(2);
            const gradeForm = new FormData();
            gradeForm.append('subjectArea', subjectArea.trim());
            gradeForm.append('file', studentFile);

            const gradeRes = await authFetch(`${API_BASE}/api/evaluate/grade-student`, {
                method: 'POST',
                body: gradeForm,
            });

            if (!gradeRes.ok) {
                const errText = await gradeRes.text();
                throw new Error(errText || `Grading failed (HTTP ${gradeRes.status})`);
            }

            const data = await gradeRes.json();
            setReportCard(data);
        } catch (err) {
            setError(err.message || 'Connection failed. Please ensure the backend server is running.');
        } finally {
            setIsProcessing(false);
            setProcessingStep(0);
        }
    };

    const handleLogout = () => {
        logout();
    };

    const handleGradeAnother = () => {
        setReportCard(null);
        setTeacherFile(null);
        setStudentFile(null);
        setSubjectArea('');
        setError(null);
    };

    const renderNavbar = () => (
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
                        <div className="profile-avatar"><User size={18} /></div>
                        <span className="profile-name">{user?.name || 'My Account'}</span>
                        <ChevronDown size={14} className={`chevron ${dropdownOpen ? 'open' : ''}`} />
                    </button>
                    {dropdownOpen && (
                        <div className="profile-dropdown">
                            <div className="dropdown-header">
                                <div className="dropdown-avatar"><User size={20} /></div>
                                <div className="dropdown-user-info">
                                    <span className="dropdown-username">{user?.name || 'User'}</span>
                                    <span className="dropdown-email">{user?.email || ''}</span>
                                </div>
                            </div>
                            <div className="dropdown-divider"></div>
                            <button className="dropdown-item logout-item" onClick={handleLogout}>
                                <LogOut size={15} /> Log Out
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );

    // Report Card view
    if (reportCard) {
        return (
            <div className="dashboard-page">
                <div className="dashboard-bg">
                    <div className="dash-orb orb-1"></div>
                    <div className="dash-orb orb-2"></div>
                </div>
                {renderNavbar()}
                <main className="dash-main dash-main-wide">
                    <ReportCard data={reportCard} onGradeAnother={handleGradeAnother} />
                </main>
                <Footer />
            </div>
        );
    }

    // Main upload view
    return (
        <div className="dashboard-page">
            <div className="dashboard-bg">
                <div className="dash-orb orb-1"></div>
                <div className="dash-orb orb-2"></div>
            </div>

            {error && (
                <div className="toast-container">
                    <div className="toast toast-error animate-slide-down">
                        <AlertCircle size={20} />
                        <div className="toast-content">
                            <span className="toast-title">Something went wrong</span>
                            <span className="toast-message">{error}</span>
                        </div>
                        <button className="toast-close" onClick={() => setError(null)}><X size={16} /></button>
                    </div>
                </div>
            )}

            {isProcessing && (
                <div className="loading-overlay">
                    <div className="loading-card">
                        <div className="loading-spinner-ring">
                            <Loader size={48} className="spin" />
                        </div>
                        <div className="loading-step-badge">Step {processingStep} / 2</div>
                        <h3 className="loading-title">
                            {processingStep === 1
                                ? 'AI is reading the Model Answer Key & building the Knowledge Graph...'
                                : "AI is reading the student's handwriting and evaluating against the Vector Graph..."}
                        </h3>
                        <p className="loading-subtitle">
                            This may take 10–30 seconds. Please don't close the page.
                        </p>
                        <div className="loading-progress-track">
                            <div
                                className="loading-progress-fill"
                                style={{ width: processingStep === 1 ? '35%' : '75%' }}
                            />
                        </div>
                        <div className="loading-dots">
                            <span></span><span></span><span></span>
                        </div>
                    </div>
                </div>
            )}

            {renderNavbar()}

            <main className="dash-main dash-main-wide">
                <div className="dash-header animate-slide-up">
                    <a href="/" className="header-logo-link">
                        <span className="logo-icon">
                            <img src={logo} alt="Eval AI Logo" />
                        </span>
                    </a>
                    <h1 className="dash-title">
                        Eval-AI <span className="text-gradient-accent">Auto-Grader</span>
                    </h1>
                    <p className="dash-subtitle">
                        Upload your model answer key and student exam script — AI grades everything in one click.
                    </p>
                </div>

                <div className="grader-card animate-slide-up">

                    <div className="form-group">
                        <label className="form-label" htmlFor="subject-input">
                            <Sparkles size={14} />
                            Subject Area
                        </label>
                        <input
                            id="subject-input"
                            type="text"
                            className="form-input"
                            placeholder='e.g. "Operating Systems", "Data Structures", "DBMS"'
                            value={subjectArea}
                            onChange={(e) => setSubjectArea(e.target.value)}
                            disabled={isProcessing}
                        />
                    </div>

                    <div className="dropzones-row">
                        <div className="dropzone-col">
                            <label className="form-label">
                                <BookOpen size={14} />
                                Teacher's Model Answer Key
                            </label>
                            {teacherFile ? (
                                <div className="file-preview">
                                    <div className="file-info">
                                        <CheckCircle size={16} className="file-check" />
                                        <div className="file-details">
                                            <span className="file-name">{teacherFile.name}</span>
                                            <span className="file-size">{(teacherFile.size / 1024).toFixed(1)} KB</span>
                                        </div>
                                    </div>
                                    <button className="remove-file" onClick={() => setTeacherFile(null)} disabled={isProcessing}>
                                        <X size={14} />
                                    </button>
                                </div>
                            ) : (
                                <label
                                    className={`upload-drop-area ${draggingZone === 'teacher' ? 'dragging' : ''}`}
                                    htmlFor="teacher-upload"
                                    onDrop={(e) => handleDrop(e, 'teacher')}
                                    onDragOver={(e) => handleDragOver(e, 'teacher')}
                                    onDragLeave={handleDragLeave}
                                >
                                    <div className="upload-icon-wrapper">
                                        <Upload size={28} className="upload-icon" />
                                    </div>
                                    <span className="upload-text">
                                        Drag & drop, or <span className="upload-link">browse</span>
                                    </span>
                                    <span className="upload-hint">PDF / PNG / JPG / WebP</span>
                                    <input
                                        id="teacher-upload"
                                        type="file"
                                        accept=".pdf,.png,.jpg,.jpeg,.webp"
                                        onChange={(e) => handleFileSelect(e, 'teacher')}
                                        hidden
                                    />
                                </label>
                            )}
                        </div>

                        <div className="dropzone-col">
                            <label className="form-label">
                                <GraduationCap size={14} />
                                Student's Handwritten Exam
                            </label>
                            {studentFile ? (
                                <div className="file-preview">
                                    <div className="file-info">
                                        <CheckCircle size={16} className="file-check" />
                                        <div className="file-details">
                                            <span className="file-name">{studentFile.name}</span>
                                            <span className="file-size">{(studentFile.size / 1024).toFixed(1)} KB</span>
                                        </div>
                                    </div>
                                    <button className="remove-file" onClick={() => setStudentFile(null)} disabled={isProcessing}>
                                        <X size={14} />
                                    </button>
                                </div>
                            ) : (
                                <label
                                    className={`upload-drop-area ${draggingZone === 'student' ? 'dragging' : ''}`}
                                    htmlFor="student-upload"
                                    onDrop={(e) => handleDrop(e, 'student')}
                                    onDragOver={(e) => handleDragOver(e, 'student')}
                                    onDragLeave={handleDragLeave}
                                >
                                    <div className="upload-icon-wrapper">
                                        <Upload size={28} className="upload-icon" />
                                    </div>
                                    <span className="upload-text">
                                        Drag & drop, or <span className="upload-link">browse</span>
                                    </span>
                                    <span className="upload-hint">PDF / PNG / JPG / WebP</span>
                                    <input
                                        id="student-upload"
                                        type="file"
                                        accept=".pdf,.png,.jpg,.jpeg,.webp"
                                        onChange={(e) => handleFileSelect(e, 'student')}
                                        hidden
                                    />
                                </label>
                            )}
                        </div>
                    </div>

                    <button
                        className={`btn-process ${isReady ? 'ready' : ''}`}
                        onClick={handleProcessAndGrade}
                        disabled={!isReady || isProcessing}
                    >
                        <Zap size={20} />
                        <span>Process & Grade Exam</span>
                    </button>

                    {!isReady && (
                        <p className="ready-hint">
                            {!subjectArea.trim() && '↑ Enter a subject area'}
                            {subjectArea.trim() && !teacherFile && !studentFile && '↑ Upload both files to continue'}
                            {subjectArea.trim() && !teacherFile && studentFile && '↑ Upload the teacher\'s model answer key'}
                            {subjectArea.trim() && teacherFile && !studentFile && '↑ Upload the student\'s exam script'}
                        </p>
                    )}
                </div>
            </main>
            <Footer />
        </div>
    );
};

export default Dashboard;