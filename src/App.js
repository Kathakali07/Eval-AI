import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Navbar from './components/navbar';
import Auth from './components/Auth';
import Description from './components/Description';
import Features from './components/features';
import HowItWorks from './components/how-it-works';
import Footer from './components/footer';

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

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Auth />} />
                <Route path="/signup" element={<Auth defaultSignup={true} />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;