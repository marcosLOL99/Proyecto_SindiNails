import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Calendar, Hammer, Users, UserCircle } from 'lucide-react';

const navLinks = [
  { to: '/admin', text: 'Dashboard', icon: <LayoutDashboard size={20} /> },
  { to: '/admin/citas', text: 'Gestionar Citas', icon: <Calendar size={20} /> },
  { to: '/admin/servicios', text: 'Gestionar Servicios', icon: <Hammer size={20} /> },
  { to: '/admin/trabajadores', text: 'Gestionar Trabajadores', icon: <Users size={20} /> },
  { to: '/admin/clientes', text: 'Gestionar Clientes', icon: <UserCircle size={20} /> },
];

const AdminLayout = () => {
  return (
    <div className="flex min-h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="w-64 bg-white shadow-md flex-shrink-0">
        <div className="p-4">
          <h2 className="text-xl font-bold text-cyan-700">Panel Admin</h2>
        </div>
        <nav className="mt-4">
          <ul>
            {navLinks.map((link) => (
              <li key={link.to}>
                <NavLink
                  to={link.to}
                  end={link.to === '/admin'} // `end` prop ensures only exact match for dashboard
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-4 py-3 text-gray-600 hover:bg-cyan-50 hover:text-cyan-600 transition-colors ${
                      isActive ? 'bg-cyan-100 text-cyan-700 font-semibold border-r-4 border-cyan-500' : ''
                    }`
                  }
                >
                  {link.icon}
                  <span>{link.text}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-6 lg:p-8">
        <div className="max-w-7xl mx-auto">
          {/* Outlet renderizará el componente de la ruta hija */}
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default AdminLayout;
