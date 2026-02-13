import axios from 'axios';

// Instance for banking services
const bankingApi = axios.create({
    baseURL: 'http://localhost:8000',
});

// Automatically inject JWT into all requests via interceptor
bankingApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export const authApi = axios.create({ baseURL: 'http://localhost:8000' });
export default bankingApi;