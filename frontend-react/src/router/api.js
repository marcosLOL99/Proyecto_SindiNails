import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'https://backend-sindinails-production.up.railway.app/api'
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const setupInterceptors = (logout) => {
  apiClient.interceptors.response.use(
    (response) => {
      return response;
    },
    (error) => {
      if (error.response && error.response.status === 401 && error.config.url !== '/auth/login') {
        console.log("Sesión expirada o token inválido. Cerrando sesión.");
        logout();
        window.location.href = '/login'; 
      }

      return Promise.reject(error);
    }
  );
};