// src/services/authService.js
import axios from 'axios';

// La URL base de tu API en el backend.
// Asegúrate de que tu backend esté corriendo en el puerto 8080.
const API_URL = 'http://localhost:8080/api/auth/';

/**
 * Llama al endpoint de login del backend.
 * @param {string} email - El email del usuario.
 * @param {string} password - La contraseña del usuario.
 * @returns {Promise<string>} El token JWT si el login es exitoso.
 */
const login = async (email, password) => {
  const response = await axios.post(API_URL + 'login', {
    email,
    password,
  });

  // Si la respuesta contiene un token, lo guardamos en localStorage.
  if (response.data.token) {
    localStorage.setItem('token', response.data.token);
  }

  return response.data;
};

/**
 * Elimina el token del localStorage para cerrar la sesión.
 */
const logout = () => {
  localStorage.removeItem('token');
};

/**
 * Obtiene el token del usuario actual desde localStorage.
 * @returns {string|null} El token JWT.
 */
const getCurrentUserToken = () => {
  return localStorage.getItem('token');
};

const AuthService = {
  login,
  logout,
  getCurrentUserToken,
};

export default AuthService;
