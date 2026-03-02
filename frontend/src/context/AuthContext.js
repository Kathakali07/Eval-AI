import React, { createContext, useContext, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const AuthContext = createContext(null);

const API_BASE = 'http://localhost:8080';

export const AuthProvider = ({ children }) => {
    const navigate = useNavigate();
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [user, setUser] = useState(() => {
        const stored = localStorage.getItem('user');
        return stored ? JSON.parse(stored) : null;
    });

    const isAuthenticated = !!token;

    const login = async (email, password) => {
        const res = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
        });

        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Invalid email or password.');
        }

        const data = await res.json();
        setToken(data.token);
        setUser({ name: data.name, email: data.email });
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({ name: data.name, email: data.email }));
        return data;
    };

    const register = async (name, email, password) => {
        const res = await fetch(`${API_BASE}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password }),
        });

        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Registration failed.');
        }

        const data = await res.json();
        setToken(data.token);
        setUser({ name: data.name, email: data.email });
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({ name: data.name, email: data.email }));
        return data;
    };

    const logout = () => {
        setToken(null);
        setUser(null);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login', { replace: true });
    };

    const value = {
        token,
        user,
        isAuthenticated,
        login,
        register,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export default AuthContext;
