import React, { useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const Dashboard = () => {
    const { user } = useContext(AuthContext);

    return (
        <div className="max-w-7xl mx-auto space-y-6">
            <header className="mb-8 border-b border-gray-200 pb-4">
                <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
            </header>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="p-6">
                        <h3 className="text-lg font-medium text-gray-900 mb-4">Profile Overview</h3>
                        <div className="space-y-3 text-sm">
                            <p className="flex justify-between items-center">
                                <span className="text-gray-500 font-medium">Username:</span> 
                                <span className="font-medium text-gray-900">{user?.username}</span>
                            </p>
                            <p className="flex justify-between items-center">
                                <span className="text-gray-500 font-medium">Role:</span> 
                                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium uppercase tracking-wide
                                    ${user?.role === 'ROLE_ADMIN' 
                                        ? 'bg-red-100 text-red-800' 
                                        : 'bg-indigo-100 text-indigo-800'}`}>
                                    {user?.role?.replace('ROLE_', '')}
                                </span>
                            </p>
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="p-6">
                        <h3 className="text-lg font-medium text-gray-900 mb-4">Student Profile</h3>
                        <p className="text-sm text-gray-600 leading-relaxed mb-4">
                            Complete your student profile to help us match you with the right projects. Add your skills, experience, and preferences.
                        </p>
                        <Link
                            to="/profile"
                            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
                        >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                            </svg>
                            View / Edit Profile
                        </Link>
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="p-6">
                        <h3 className="text-lg font-medium text-gray-900 mb-4">Coming Soon</h3>
                        <p className="text-sm text-gray-600 leading-relaxed">
                            Project matching and team formation features are coming in Phase 5+. Stay tuned!
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
