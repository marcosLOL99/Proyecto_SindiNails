import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { motion } from "framer-motion";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function Registro() {
  const [formData, setFormData] = useState({
    nombre: "",
    apellidos: "",
    email: "",
    telefono: "",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { register, isAuthenticated } = useAuth(); // Añadimos register del contexto
  const navigate = useNavigate();

  // Si el usuario ya está logueado, lo redirigimos para que no vea esta página
  useEffect(() => {
    if (isAuthenticated) {
      navigate("/dashboard");
    }
  }, [isAuthenticated, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.id]: e.target.value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");

    if (formData.password !== formData.confirmPassword) {
      setError("Las contraseñas no coinciden.");
      return;
    }

    setLoading(true);
    try {
      // Creamos una función de registro en el contexto para mantener la lógica centralizada
      await register(
        formData.nombre,
        formData.apellidos,
        formData.email,
        formData.telefono,
        formData.password
      );
      navigate("/dashboard"); // Redirige al dashboard tras el registro exitoso
    } catch (err) {
      const errorMessage = err.response?.data?.message || "Error al registrar la cuenta. Inténtalo más tarde.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100 dark:bg-gray-900 py-12">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md"
      >
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">Crear una Cuenta</CardTitle>
            <CardDescription>
              Introduce tus datos para registrarte en SindiNails.
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleRegister}>
            <CardContent className="grid gap-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2"><Label htmlFor="nombre">Nombre</Label><Input id="nombre" required onChange={handleChange} /></div>
                <div className="grid gap-2"><Label htmlFor="apellidos">Apellidos</Label><Input id="apellidos" required onChange={handleChange} /></div>
              </div>
              <div className="grid gap-2"><Label htmlFor="email">Email</Label><Input id="email" type="email" required onChange={handleChange} /></div>
              <div className="grid gap-2"><Label htmlFor="telefono">Teléfono</Label><Input id="telefono" type="tel" required onChange={handleChange} /></div>
              <div className="grid gap-2"><Label htmlFor="password">Contraseña</Label><Input id="password" type="password" required onChange={handleChange} /></div>
              <div className="grid gap-2"><Label htmlFor="confirmPassword">Confirmar Contraseña</Label><Input id="confirmPassword" type="password" required onChange={handleChange} /></div>
              {error && <p className="text-sm font-medium text-destructive">{error}</p>}
            </CardContent>
            <CardFooter className="flex flex-col">
              <Button className="w-full bg-cyan-600 hover:bg-cyan-700" type="submit" disabled={loading}>
                {loading ? "Creando cuenta..." : "Registrarse"}
              </Button>
              <div className="mt-4 text-center text-sm">
                ¿Ya tienes una cuenta?{" "}
                <Link to="/login" className="underline">
                  Inicia sesión
                </Link>
              </div>
            </CardFooter>
          </form>
        </Card>
      </motion.div>
    </div>
  );
}