import React from 'react';
import { FaLinkedin, FaGithub } from 'react-icons/fa';
import './footer.css';

const teamMembers = [
    {
        name: 'Satyam Puitandy',
        email: 'puitandys05@gmail.com',
        linkedin: 'https://linkedin.com/in/satyampuitandy',
        github: 'https://github.com/puitandysatyam',
    },
    {
        name: 'Rabishankar Roy',
        email: 'rabishankarroy04@gmail.com',
        linkedin: 'https://linkedin.com/in/rabishankar-roy-055a52343',
        github: 'https://github.com/rabishankarroy04-svg',
    },
    {
        name: 'Kathakali Das',
        email: '2004kathakali@gmail.com',
        linkedin: 'https://linkedin.com/in/kathakali-kd-46a93623b',
        github: 'https://github.com/Kathakali07',
    },
    {
        name: 'Saheli Panja',
        email: 'sahelipanja0@gmail.com',
        linkedin: 'https://linkedin.com/in/sahelipanja',
        github: 'https://github.com/sahelipanja',
    },
];

const Footer = () => {
    return (
        <footer className="footer" id="footer">
            <div className="container">
                <div className="footer-content">

                    <div className="footer-brand">
                        <a href="/" className="logo footer-logo">
                            <span className="logo-text">
                                OUR <span className="text-gradient-accent">TEAM</span>
                            </span>
                        </a>

                        <div className="team-section">
                            <div className="team-list">
                                {teamMembers.map((member, index) => (
                                    <div className="team-card" key={index}>
                                        <div className="team-card-info">
                                            <span className="team-card-name">{member.name}</span>
                                            <a href={`mailto:${member.email}`} className="team-card-email">
                                                {member.email}
                                            </a>
                                        </div>
                                        <div className="team-card-icons">
                                            <a href={member.linkedin} target="_blank" rel="noreferrer" aria-label={`${member.name} LinkedIn`} className="team-icon-link">
                                                <FaLinkedin size={16} />
                                            </a>
                                            <a href={member.github} target="_blank" rel="noreferrer" aria-label={`${member.name} GitHub`} className="team-icon-link">
                                                <FaGithub size={16} />
                                            </a>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="footer-links-group">
                        <h4 className="footer-heading">PRODUCT</h4>
                        <a href="#features" className="footer-link">Features</a>
                        <a href="#how-it-works" className="footer-link">How it Works</a>
                        <a href="/pricing" className="footer-link">Pricing</a>
                        <a href="/api" className="footer-link">API Docs</a>
                    </div>

                    <div className="footer-links-group">
                        <h4 className="footer-heading">COMPANY</h4>
                        <a href="/about" className="footer-link">About Us</a>
                        <a href="/blog" className="footer-link">Blog</a>
                        <a href="/careers" className="footer-link">Careers</a>
                        <a href="/contact" className="footer-link">Contact</a>
                    </div>

                    <div className="footer-links-group">
                        <h4 className="footer-heading">LEGAL</h4>
                        <a href="/privacy" className="footer-link">Privacy Policy</a>
                        <a href="/terms" className="footer-link">Terms of Service</a>
                        <a href="/security" className="footer-link">Security</a>
                    </div>

                </div>

                <div className="footer-bottom">
                    <p>&copy; {new Date().getFullYear()} Eval AI. All rights reserved.</p>
                </div>
            </div>
        </footer>
    );
};

export default Footer;