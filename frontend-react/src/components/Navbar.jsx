import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  NavigationMenu,
  NavigationMenuList,
  NavigationMenuItem,
  NavigationMenuTrigger,
  NavigationMenuContent,
} from "@/components/ui/navigation-menu"; // Ajusta la ruta según tu proyecto
import { Menu, X, Home, Hammer, Phone, LogIn } from "lucide-react";

export default function Navbar() {
  const location = useLocation();
  const [open, setOpen] = useState(false);

  const navItems = [
    { name: "Inicio", path: "/", icon: <Home size={18} /> },
    { name: "Servicios", path: "/servicios", icon: <Hammer size={18} /> },
    { name: "Contacto", path: "/contacto", icon: <Phone size={18} /> },
    { name: "Login", path: "/login", icon: <LogIn size={18} /> },
  ];

  return (
    <nav className="bg-white shadow-md fixed w-full z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <Link to="/" className="flex-shrink-0 font-bold text-xl text-black">
            MiSitio
          </Link>

          {/* Menu de escritorio */}
          <div className="hidden md:flex space-x-6">
            {navItems.map((item) => (
              <Link
                key={item.name}
                to={item.path}
                className={`flex items-center gap-1 px-3 py-2 rounded-md text-sm font-medium ${
                  location.pathname === item.path
                    ? "bg-black text-white"
                    : "text-black hover:bg-gray-200"
                }`}
              >
                {item.icon}
                {item.name}
              </Link>
            ))}
          </div>

          {/* Botón de menú móvil */}
          <div className="md:hidden">
            <button
              onClick={() => setOpen(!open)}
              className="text-black focus:outline-none"
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
                    ? "bg-black text-white"
                    : "text-black hover:bg-gray-200"
                }`}
              >
                {item.icon}
                {item.name}
              </Link>
            ))}
          </div>
        </div>
      )}
    </nav>
  );
}
