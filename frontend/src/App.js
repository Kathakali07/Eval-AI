import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/navbar';
import Auth from './components/Auth';
import Description from './components/Description';
import Features from './components/features';
import HowItWorks from './components/how-it-works';
import Footer from './components/footer';
import Dashboard from './components/Dashboard';

const Home = () => (
    <div className="app-container">
        <Navbar />
        <main>
            <Description />
            <Features />
            <HowItWorks />
        </main>
        <Footer />
    </div>
);

/** Redirect authenticated users away from login/signup screens. */
const PublicOnlyRoute = ({ children }) => {
    const { isAuthenticated } = useAuth();
    if (isAuthenticated) {
        return <Navigate to="/dashboard" replace />;
    }
    return children;
};

/** Redirect unauthenticated users to the login page. */
const PrivateRoute = ({ children }) => {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }
    return children;
};

function AppRoutes() {
    return (
        <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={
                <PublicOnlyRoute>
                    <Auth />
                </PublicOnlyRoute>
            } />
            <Route path="/signup" element={
                <PublicOnlyRoute>
                    <Auth defaultSignup={true} />
                </PublicOnlyRoute>
            } />
            <Route path="/dashboard" element={
                <PrivateRoute>
                    <Dashboard />
                </PrivateRoute>
            } />
        </Routes>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AppRoutes />
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;