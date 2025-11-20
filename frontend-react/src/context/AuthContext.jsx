import { createContext, useState, useContext, useEffect, useCallback } from 'react';
import { apiClient, setupInterceptors } from '../router/api.js';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  const fetchUser = useCallback(async () => {
    if (localStorage.getItem('token')) {
      try {
        const response = await apiClient.get('/users/me');
        setUser(response.data);
        setIsAuthenticated(true);
      } catch (error) {
        console.error("Error fetching user, logging out", error);
      }
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  const login = async (email, password) => {
    const response = await apiClient.post('/auth/login', { email, password });
    const { token } = response.data;
    if (token) {
      localStorage.setItem('token', token);
      setToken(token);
      await fetchUser();
    }
    return response;
  };

  const register = async (nombre, apellidos, email, telefono, password) => {
    const response = await apiClient.post('/auth/register', { nombre, apellidos, email, telefono, password });
    const { token } = response.data;
    if (token) {
      localStorage.setItem('token', token);
      setToken(token);
      await fetchUser();
    }
    return response;
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    setIsAuthenticated(false);
  };

  const updateUser = async (userData) => {
    const response = await apiClient.put('/users/me', userData);
    setUser(response.data);
  };

  useEffect(() => {
    setupInterceptors(logout);
  }, [logout]);

  const value = {
    token,
    user,
    isAuthenticated,
    loading,
    login,
    register,
    logout,
    updateUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser usado dentro de un AuthProvider');
  }
  return context;
};