import axios from 'axios';

// Instance for banking services
const bankingApi = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1/banking',
});

// Automatically inject JWT into all requests via interceptor
bankingApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export const authApi = axios.create({
    baseURL: import.meta.env.VITE_AUTH_URL || 'http://localhost:8000/api/v1/auth',
});
export default bankingApi;