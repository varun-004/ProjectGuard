import api from './api';

const mlModelService = {
    /**
     * Fetch model status (availability, active version, algorithm, metrics).
     * Accessible by both STUDENT and ADMIN roles.
     */
    getModelStatus: () => api.get('/ml-model/status'),

    /**
     * Trigger authoritative Phase 10 training pipeline.
     * Evaluates dataset readiness (>= 50 authentic records, both classes).
     * ADMIN only.
     */
    trainModel: () => api.post('/ml-model/train'),

    /**
     * Get detailed evaluation metrics and feature importance.
     * ADMIN only.
     */
    getModelMetrics: (version = '') => {
        const query = version ? `?version=${encodeURIComponent(version)}` : '';
        return api.get(`/ml-model/metrics${query}`);
    },

    /**
     * Predict project feasibility (FEASIBLE / NOT_FEASIBLE) with risk factors.
     * Accessible by both STUDENT and ADMIN roles.
     */
    predict: (payload) => api.post('/ml-model/predict', payload),

    /**
     * List all saved model versions in the registry.
     * ADMIN only.
     */
    listVersions: () => api.get('/ml-model/versions'),

    /**
     * Activate a specific model version.
     * ADMIN only.
     */
    activateVersion: (version) => api.post(`/ml-model/activate/${encodeURIComponent(version)}`)
};

export default mlModelService;

