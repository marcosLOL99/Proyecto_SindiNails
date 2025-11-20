import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Menu, X, Home, Hammer, Phone, LogIn, User, LogOut, LayoutDashboard, UserPlus } from "lucide-react";

export default function Navbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const { isAuthenticated, user, logout } = useAuth();

  const navItems = [
    { name: "Inicio", path: "/", icon: <Home size={18} /> },
    { name: "Servicios", path: "/servicios", icon: <Hammer size={18} /> },
    { name: "Contacto", path: "/contacto", icon: <Phone size={18} /> },
  ];

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <nav className="bg-white shadow-md fixed w-full z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <Link to="/" className="flex-shrink-0 font-bold text-xl text-black">
            SindiNails
          </Link>

          {/* Menu de escritorio */}
          <div className="hidden md:flex items-center space-x-4">
            <div className="flex space-x-1">
              {navItems.map((item) => (
                <Link
                  key={item.name}
                  to={item.path}
                  className={`flex items-center gap-1 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    location.pathname === item.path
                      ? "bg-cyan-600 text-white"
                      : "text-gray-700 hover:bg-cyan-500 hover:text-white"
                  }`}
                >
                  {item.icon}
                  {item.name}
                </Link>
              ))}
            </div>

            {isAuthenticated ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" className="flex items-center gap-2 border-cyan-600 text-cyan-600 hover:bg-cyan-600 hover:text-white">
                    <User size={18} />
                    Hola, {user?.nombre}
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Mi Cuenta</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem asChild className="cursor-pointer">
                    <Link to="/dashboard"><LayoutDashboard className="mr-2 h-4 w-4" />Mi Panel</Link>
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-red-500 focus:text-red-500 focus:bg-red-50">
                    <LogOut className="mr-2 h-4 w-4" />
                    Cerrar Sesión
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <div className="flex items-center space-x-2">
                <Button asChild variant="ghost" className="text-cyan-600 hover:bg-cyan-100 hover:text-cyan-700">
                  <Link to="/login"><LogIn className="mr-2 h-4 w-4" />Iniciar Sesión</Link>
                </Button>
                <Button asChild className="bg-cyan-600 hover:bg-cyan-700">
                  <Link to="/registro"><UserPlus className="mr-2 h-4 w-4" />Registrarse</Link>
                </Button>
              </div>
            )}
          </div>

          {/* Botón de menú móvil */}
          <div className="md:hidden">
            <button
              onClick={() => setOpen(!open)}
              className="text-gray-700 focus:outline-none"
            >
              {open ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </div>

      {/* Menú móvil */}
      {open && (
        <div className="md:hidden bg-white shadow-md">
          <div className="px-2 pt-2 pb-3 space-y-1">
            {navItems.map((item) => (
              <Link
                key={item.name}
                to={item.path}
                onClick={() => setOpen(false)}
                className={`flex items-center gap-2 px-3 py-2 rounded-md text-base font-medium ${
                  location.pathname === item.path
                    ? "bg-cyan-600 text-white"
                    : "text-gray-700 hover:bg-gray-100"
                }`}
              >
                {item.icon}
                {item.name}
              </Link>
            ))}
            <hr className="my-2 border-gray-200" />
            {isAuthenticated ? (
              <>
                <Link to="/dashboard" onClick={() => setOpen(false)} className="flex items-center gap-2 px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"><LayoutDashboard size={18} />Mi Panel</Link>
                <button onClick={() => { handleLogout(); setOpen(false); }} className="w-full text-left flex items-center gap-2 px-3 py-2 rounded-md text-base font-medium text-red-500 hover:bg-red-50">
                  <LogOut size={18} />Cerrar Sesión
                </button>
              </>
            ) : (
              <>
                <Link to="/login" onClick={() => setOpen(false)} className="flex items-center gap-2 px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100">
                  <LogIn size={18} />Iniciar Sesión
                </Link>
                <Link to="/registro" onClick={() => setOpen(false)} className="flex items-center gap-2 px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100">
                  <UserPlus size={18} />Registrarse
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
