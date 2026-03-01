import React, { useState } from 'react';
import { Mail, Lock, LogIn, GraduationCap, User, UserPlus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import logo from './Eval-AI-Logo-only.png';
import './Auth.css';

const Auth = ({ defaultSignup = false }) => {
    const navigate = useNavigate();
    const [isLogin, setIsLogin] = useState(!defaultSignup);
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!isLogin && formData.password !== formData.confirmPassword) {
            alert("Passwords do not match!");
            return;
        }

        if (isLogin) {
            // ─────────────────────────────────────────
            // TODO: Backend login API call goes here
            // e.g. await axios.post('/api/auth/login', {
            //     email: formData.email,
            //     password: formData.password
            // });
            // ─────────────────────────────────────────
            navigate('/dashboard');
        } else {
            // ─────────────────────────────────────────
            // TODO: Backend signup API call goes here
            // e.g. await axios.post('/api/auth/signup', {
            //     username: formData.username,
            //     email: formData.email,
            //     password: formData.password
            // });
            // ─────────────────────────────────────────
            navigate('/dashboard');
        }
    };

    const handleForgotPassword = (e) => {
        e.preventDefault();
        // ─────────────────────────────────────────
        // TODO: Backend forgot password API call goes here
        // e.g. await axios.post('/api/auth/forgot-password', {
        //     email: formData.email
        // });
        // ─────────────────────────────────────────
    };

    const toggleAuth = () => {
        setIsLogin(!isLogin);
        setFormData({
            username: '',
            email: '',
            password: '',
            confirmPassword: ''
        });
    };

    return (
        <div className="login-page">
            <div className="bg-decor decor-1"></div>
            <div className="bg-decor decor-2"></div>

            <div className="login-container">
                <div className="glass-card">

                    {/* ── Brand ── */}
                    <div className="brand">
                        <div className="brand-icon">
                            <img src={logo} alt="Eval AI Logo" style={{ width: '80px', height: '80px', objectFit: 'contain' }} />
                        </div>
                        <h1>{isLogin ? 'Welcome Back' : 'Create Account'}</h1>
                        <p>{isLogin ? 'Sign in to continue to EvalAI' : 'Join EvalAI today'}</p>
                    </div>

                    {/* ── Form ── */}
                    <div className="auth-form">

                        {/* Username — signup only */}
                        {!isLogin && (
                            <div className="form-group">
                                <label>Username</label>
                                <div className="input-wrapper">
                                    <i><User size={18} /></i>
                                    <input
                                        name="username"
                                        type="text"
                                        placeholder="Your username"
                                        value={formData.username}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>
                            </div>
                        )}

                        {/* Email */}
                        <div className="form-group">
                            <label>Email Address</label>
                            <div className="input-wrapper">
                                <i><Mail size={18} /></i>
                                <input
                                    name="email"
                                    type="email"
                                    placeholder="123@gmail.com"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                        </div>

                        {/* Password */}
                        <div className="form-group">
                            <label>Password</label>
                            <div className="input-wrapper">
                                <i><Lock size={18} /></i>
                                <input
                                    name="password"
                                    type="password"
                                    placeholder="••••••••"
                                    value={formData.password}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                        </div>

                        {/* Confirm Password — signup only */}
                        {!isLogin && (
                            <div className="form-group">
                                <label>Confirm Password</label>
                                <div className="input-wrapper">
                                    <i><Lock size={18} /></i>
                                    <input
                                        name="confirmPassword"
                                        type="password"
                                        placeholder="••••••••"
                                        value={formData.confirmPassword}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>
                            </div>
                        )}

                        {/* Remember me & Forgot password — login only */}
                        {isLogin && (
                            <div className="options">
                                <label className="remember-me">
                                    <input type="checkbox" />
                                    <span>Remember me</span>
                                </label>
                                <a href="#" className="forgot-password" onClick={handleForgotPassword}>Forgot password?</a>
                            </div>
                        )}

                        {/* Submit */}
                        <button className="btn-primary" onClick={handleSubmit}>
                            {isLogin ? <LogIn size={18} /> : <UserPlus size={18} />}
                            {isLogin ? 'Sign In' : 'Sign Up'}
                        </button>

                    </div>

                    {/* ── Toggle Login / Signup ── */}
                    <p className="footer-text">
                        {isLogin ? "Don't have an account?" : "Already have an account?"}
                        <a href="#" onClick={(e) => { e.preventDefault(); toggleAuth(); }}>{isLogin ? ' Sign Up' : ' Sign In'}</a>
                    </p>

                </div>
            </div>
        </div>
    );
};

export default Auth;