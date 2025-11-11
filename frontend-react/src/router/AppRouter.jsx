import { createBrowserRouter } from 'react-router-dom';

import App from '../App.jsx';
import Home from '../pages/publico/Home.jsx';
import Servicios from '../pages/publico/Servicios.jsx';
import Contacto from '../pages/publico/Contacto.jsx';
import Login from '../pages/publico/Login.jsx';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Home /> },
      { path: 'servicios', element: <Servicios /> },
      { path: 'contacto', element: <Contacto /> },
      { path: 'login', element: <Login /> },
    ],
  },
]);

export default router;
