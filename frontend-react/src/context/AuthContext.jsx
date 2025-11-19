import { createContext, useState, useContext, useEffect } from 'react';
import apiClient from '../router/api.js';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    if (storedToken) {
      setToken(storedToken);
      setIsAuthenticated(true);
    }
  }, []);

  const login = async (email, password) => {
    const response = await apiClient.post('/auth/login', { email, password });
    const { token } = response.data;
    if (token) {
      localStorage.setItem('token', token);
      setToken(token);
      setIsAuthenticated(true);
    }
    return response;
  };

  const register = async (nombre, apellidos, email, telefono, password) => {
    const response = await apiClient.post('/auth/register', { nombre, apellidos, email, telefono, password });
    const { token } = response.data;
    // El backend actual devuelve un token al registrar, así que lo manejamos.
    if (token) {
      localStorage.setItem('token', token);
      setToken(token);
      setIsAuthenticated(true);
    }
    return response;
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setIsAuthenticated(false);
  };

  const value = {
    token,
    isAuthenticated,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser usado dentro de un AuthProvider');
  }
  return context;
};