import api from './api';

const adminService = {
    /**
     * GET /api/admin/stats — aggregate platform statistics
     */
    getStats: async () => {
        return api.get('/admin/stats');
    },
};

export default adminService;
