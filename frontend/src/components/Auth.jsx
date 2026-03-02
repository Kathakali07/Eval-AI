import React, { useState } from 'react';
import { Mail, Lock, LogIn, User, UserPlus, Loader } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import logo from './Eval-AI-Logo-only.png';
import './Auth.css';

const Auth = ({ defaultSignup = false }) => {
    const navigate = useNavigate();
    const { login, register } = useAuth();
    const [isLogin, setIsLogin] = useState(!defaultSignup);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError(null);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (!formData.email || !formData.password) {
            setError('Please fill in all required fields.');
            return;
        }

        if (!isLogin && formData.password !== formData.confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        setLoading(true);

        try {
            if (isLogin) {
                await login(formData.email, formData.password);
            } else {
                await register(formData.username, formData.email, formData.password);
            }
            navigate('/dashboard');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const toggleAuth = () => {
        setIsLogin(!isLogin);
        setError(null);
        setFormData({
            username: '',
            email: '',
            password: '',
            confirmPassword: ''
        });
    };

    return (
        <div className="login-page">
            <div className="bg-decor">
                <div className="glow-orb orb-1"></div>
                <div className="glow-orb orb-2"></div>
            </div>

            <div className="login-container">
                <div className="glass-card">

                    <div className="brand">
                        <div className="brand-icon">
                            <img src={logo} alt="Eval AI Logo" style={{ width: '80px', height: '80px', objectFit: 'contain' }} />
                        </div>
                        <h1>{isLogin ? 'Welcome Back' : 'Create Account'}</h1>
                        <p>{isLogin ? 'Sign in to continue to EvalAI' : 'Join EvalAI today'}</p>
                    </div>

                    {error && (
                        <div className="auth-error">
                            {error}
                        </div>
                    )}

                    <div className="auth-form">

                        {!isLogin && (
                            <div className="form-group">
                                <label>Full Name</label>
                                <div className="input-wrapper">
                                    <i><User size={18} /></i>
                                    <input
                                        name="username"
                                        type="text"
                                        placeholder="Your full name"
                                        value={formData.username}
                                        onChange={handleChange}
                                        required
                                        disabled={loading}
                                    />
                                </div>
                            </div>
                        )}

                        <div className="form-group">
                            <label>Email Address</label>
                            <div className="input-wrapper">
                                <i><Mail size={18} /></i>
                                <input
                                    name="email"
                                    type="email"
                                    placeholder="you@email.com"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                    disabled={loading}
                                />
                            </div>
                        </div>

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
                                    disabled={loading}
                                />
                            </div>
                        </div>

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
                                        disabled={loading}
                                    />
                                </div>
                            </div>
                        )}

                        <button className="btn-primary" onClick={handleSubmit} disabled={loading}>
                            {loading ? (
                                <><Loader size={18} className="spin" /> Processing...</>
                            ) : (
                                <>{isLogin ? <LogIn size={18} /> : <UserPlus size={18} />} {isLogin ? 'Sign In' : 'Sign Up'}</>
                            )}
                        </button>

                    </div>

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