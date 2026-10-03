import React, { useContext, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const Navbar = () => {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    const location = useLocation();
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

    const navLinks = [
        { to: '/dashboard', label: 'Dashboard', icon: '⊞' },
        { to: '/profile',   label: 'My Profile', icon: '◎' },
        { to: '/assessments', label: 'Assessments', icon: '✦' },
    ];

    return (
        <nav className="sticky top-0 z-40 bg-white/90 backdrop-blur-md border-b border-gray-200/80 shadow-sm">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between h-16">
                    {/* Logo */}
                    <div className="flex items-center">
                        <Link to="/" className="flex items-center gap-2 group">
                            <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-md shadow-indigo-200 group-hover:shadow-indigo-300 transition-shadow">
                                <span className="text-white font-bold text-sm">PG</span>
                            </div>
                            <span className="text-lg font-bold bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
                                ProjectGuard
                            </span>
                        </Link>
                    </div>

                    {/* Desktop Menu */}
                    <div className="hidden md:flex md:items-center md:gap-1">
                        {user ? (
                            <>
                                {navLinks.map(({ to, label }) => (
                                    <Link
                                        key={to}
                                        to={to}
                                        className={`relative px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200
                                            ${isActive(to)
                                                ? 'text-indigo-700 bg-indigo-50'
                                                : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-50'
                                            }`}
                                    >
                                        {label}
                                        {isActive(to) && (
                                            <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-4 h-0.5 rounded-full bg-indigo-500" />
                                        )}
                                    </Link>
                                ))}

                                <div className="ml-4 pl-4 border-l border-gray-200 flex items-center gap-3">
                                    <div className="flex items-center gap-2">
                                        <div className="h-8 w-8 rounded-full bg-gradient-to-br from-indigo-400 to-violet-500 flex items-center justify-center shadow-sm">
                                            <span className="text-white text-xs font-bold uppercase">
                                                {user.username?.charAt(0)}
                                            </span>
                                        </div>
                                        <span className="text-sm font-medium text-gray-700">{user.username}</span>
                                    </div>
                                    <button
                                        onClick={handleLogout}
                                        className="px-3 py-1.5 text-sm font-medium text-gray-600 rounded-lg border border-gray-200 hover:border-red-200 hover:text-red-600 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all"
                                    >
                                        Sign out
                                    </button>
                                </div>
                            </>
                        ) : (
                            <div className="flex items-center gap-3">
                                <Link
                                    to="/login"
                                    className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-indigo-600 rounded-lg hover:bg-gray-50 transition-colors"
                                >
                                    Sign in
                                </Link>
                                <Link
                                    to="/register"
                                    className="px-4 py-2 text-sm font-semibold text-white bg-gradient-to-r from-indigo-600 to-violet-600 rounded-lg hover:from-indigo-700 hover:to-violet-700 shadow-sm shadow-indigo-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all"
                                >
                                    Get Started
                                </Link>
                            </div>
                        )}
                    </div>

                    {/* Mobile hamburger */}
                    <div className="flex items-center md:hidden">
                        <button
                            onClick={() => setIsMenuOpen(!isMenuOpen)}
                            className="inline-flex items-center justify-center p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-500 transition-colors"
                            aria-label="Toggle menu"
                        >
                            {isMenuOpen ? (
                                <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            ) : (
                                <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                                </svg>
                            )}
                        </button>
                    </div>
                </div>
            </div>

            {/* Mobile Menu */}
            <div className={`md:hidden transition-all duration-200 overflow-hidden ${isMenuOpen ? 'max-h-80 opacity-100' : 'max-h-0 opacity-0'}`}>
                <div className="px-4 py-3 space-y-1 bg-white border-t border-gray-100">
                    {user ? (
                        <>
                            {/* User badge */}
                            <div className="flex items-center gap-3 px-3 py-3 mb-2 bg-gray-50 rounded-xl">
                                <div className="h-9 w-9 rounded-full bg-gradient-to-br from-indigo-400 to-violet-500 flex items-center justify-center shadow-sm">
                                    <span className="text-white text-sm font-bold uppercase">{user.username?.charAt(0)}</span>
                                </div>
                                <div>
                                    <p className="text-sm font-semibold text-gray-800">{user.username}</p>
                                    <p className="text-xs text-gray-500">{user.role?.replace('ROLE_', '')}</p>
                                </div>
                            </div>

                            {navLinks.map(({ to, label }) => (
                                <Link
                                    key={to}
                                    to={to}
                                    onClick={() => setIsMenuOpen(false)}
                                    className={`flex items-center px-4 py-2.5 rounded-lg text-sm font-medium transition-colors
                                        ${isActive(to)
                                            ? 'bg-indigo-50 text-indigo-700'
                                            : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
                                        }`}
                                >
                                    {label}
                                </Link>
                            ))}

                            <div className="pt-2 border-t border-gray-100">
                                <button
                                    onClick={handleLogout}
                                    className="w-full flex items-center px-4 py-2.5 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 transition-colors"
                                >
                                    Sign out
                                </button>
                            </div>
                        </>
                    ) : (
                        <div className="flex flex-col gap-2 py-2">
                            <Link
                                to="/login"
                                onClick={() => setIsMenuOpen(false)}
                                className="block px-4 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                            >
                                Sign in
                            </Link>
                            <Link
                                to="/register"
                                onClick={() => setIsMenuOpen(false)}
                                className="block text-center px-4 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-indigo-600 to-violet-600 rounded-lg"
                            >
                                Get Started
                            </Link>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
};

export default Navbar;
