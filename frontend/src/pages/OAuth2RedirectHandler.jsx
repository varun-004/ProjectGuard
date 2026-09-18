import React, { useEffect, useContext } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const OAuth2RedirectHandler = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { googleLogin } = useContext(AuthContext);

    useEffect(() => {
        const token = searchParams.get('token');
        const username = searchParams.get('username');
        const role = searchParams.get('role');

        if (token && username && role) {
            googleLogin(token, username, role);
            navigate('/dashboard', { replace: true });
        } else {
            navigate('/login', { state: { error: 'Google login failed' }, replace: true });
        }
    }, [searchParams, navigate, googleLogin]);

    return (
        <div className="flex items-center justify-center min-h-[calc(100vh-8rem)]">
            <div className="text-xl font-medium text-gray-500 flex flex-col items-center gap-3">
                <svg className="animate-spin h-8 w-8 text-indigo-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Authenticating with Google...</span>
            </div>
        </div>
    );
};

export default OAuth2RedirectHandler;
