import { createBrowserRouter } from 'react-router-dom';

import App from '../App.jsx';
import Home from '../pages/publico/Home.jsx';
import Servicios from '../pages/publico/Servicios.jsx';
import Contacto from '../pages/publico/Contacto.jsx';
import Login from '../pages/publico/Login.jsx';
import Registro from '../pages/publico/Registro.jsx';
import ProtectedRoute from './ProtectedRoute.jsx';
import AdminRoute from '../context/AdminRoute.jsx';
import Dashboard from '../pages/cliente/Dashboard.jsx';
import ReservarCita from '../pages/cliente/ReservarCita.jsx';
import AdminLayout from '../pages/admin/AdminLayout.jsx';
import AdminDashboard from '../pages/admin/AdminDashboard.jsx';
import AdminGestionCitas from '../pages/admin/AdminGestionCitas.jsx';
import AdminGestionServicios from '../pages/admin/AdminGestionServicios.jsx';
import AdminGestionTrabajadores from '../pages/admin/AdminGestionTrabajadores.jsx';
import AdminGestionClientes from '../pages/admin/AdminGestionClientes.jsx';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Home /> },
      { path: 'servicios', element: <Servicios /> },
      { path: 'contacto', element: <Contacto /> },
      { path: 'login', element: <Login /> },
      { path: 'registro', element: <Registro /> },
      // Rutas Protegidas
      {
        element: <ProtectedRoute />,
        children: [
          { path: 'dashboard', element: <Dashboard /> },
          { path: 'reservar/:servicioId', element: <ReservarCita /> },
        ],
      },
      // Rutas de Administrador
      {
        path: 'admin',
        element: <AdminRoute><AdminLayout /></AdminRoute>,
        children: [
          { index: true, element: <AdminDashboard /> },
          { path: 'citas', element: <AdminGestionCitas /> },
          { path: 'servicios', element: <AdminGestionServicios /> },
          { path: 'trabajadores', element: <AdminGestionTrabajadores /> },
          { path: 'clientes', element: <AdminGestionClientes /> },
        ],
      },
    ],
  },
]);

export default router;
