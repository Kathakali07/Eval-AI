import React, { useState, useEffect } from 'react';
import './navbar.css';
import { Link } from 'react-router-dom';
import logo from './Eval-AI-Logo-only.png';

const Navbar = () => {
    const [scrolled, setScrolled] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 20);
        };
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    return (
        <nav className={`navbar ${scrolled ? 'scrolled' : ''}`}>
            <div className="container nav-container">
                <a href="/" className="logo">
                    <span className="logo-icon">
                        <img src={logo} alt="Eval AI Logo" />
                    </span>
                    <span className="logo-text">Eval <span className="text-gradient-accent">AI</span></span>
                </a>

                <div className="nav-links">
                    <a href="#features" className="nav-link">Features</a>
                    <a href="#how-it-works" className="nav-link">How it Works</a>
                    <a href="#footer" className="nav-link">Contact Us</a>
                </div>

                <div className="nav-actions">
                    <Link to="/login" className="btn btn-outline">Log In</Link>
                    <a href="/signup" className="btn btn-primary">Get Started</a>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;