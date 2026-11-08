import api from './api';

const mlDatasetService = {
    getStatistics: () => api.get('/ml-dataset/statistics'),

    getRecords: (page = 0, size = 15, outcomeStatus = '') => {
        let url = `/ml-dataset?page=${page}&size=${size}`;
        if (outcomeStatus && outcomeStatus !== 'ALL') {
            url += `&outcomeStatus=${outcomeStatus}`;
        }
        return api.get(url);
    },

    getPendingOutcomes: (page = 0, size = 15) => {
        return api.get(`/ml-dataset/pending-outcomes?page=${page}&size=${size}`);
    },

    recordActualOutcome: (id, payload) => {
        return api.put(`/ml-dataset/${id}/outcome`, payload);
    },

    prepareDataset: () => {
        return api.post('/ml-dataset/prepare');
    },

    getSplitReport: () => {
        return api.get('/ml-dataset/split');
    },

    registerProject: (payload) => {
        return api.post('/ml-dataset/register', payload);
    },

    downloadCleanedCsv: async () => {
        const response = await api.get('/ml-dataset/export', {
            responseType: 'blob'
        });
        const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'projectguard_ml_dataset.csv');
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
    }
};

export default mlDatasetService;

