import React, { createContext, useState, useEffect } from 'react';
import api from '../services/api';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token') || null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // Initialize user from local storage token data if available
        // Normally we'd fetch /api/auth/me, but we only have token/username/role in AuthResponse
        const storedUsername = localStorage.getItem('username');
        const storedRole = localStorage.getItem('role');
        
        if (token && storedUsername && storedRole) {
            setUser({ username: storedUsername, role: storedRole });
        }
        setIsLoading(false);
    }, [token]);

    const login = async (username, password) => {
        try {
            const response = await api.post('/auth/login', { username, password });
            const { token, username: resUsername, role } = response.data;
            
            localStorage.setItem('token', token);
            localStorage.setItem('username', resUsername);
            localStorage.setItem('role', role);
            
            setToken(token);
            setUser({ username: resUsername, role });
            return { success: true };
        } catch (error) {
            return { success: false, message: error.response?.data?.message || 'Login failed' };
        }
    };

    const register = async (username, email, password) => {
        try {
            const response = await api.post('/auth/register', { username, email, password });
            return { success: true };
        } catch (error) {
            return { success: false, message: error.response?.data?.message || 'Registration failed' };
        }
    };

    const googleLogin = (token, username, role) => {
        localStorage.setItem('token', token);
        localStorage.setItem('username', username);
        localStorage.setItem('role', role);
        
        setToken(token);
        setUser({ username, role });
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, token, isLoading, login, register, googleLogin, logout }}>
            {children}
        </AuthContext.Provider>
    );
};
