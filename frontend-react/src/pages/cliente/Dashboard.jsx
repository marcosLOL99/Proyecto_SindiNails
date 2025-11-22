import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { apiClient } from "../../router/api.js";
import { format, parseISO } from 'date-fns';
import { es } from 'date-fns/locale';

// Componentes UI y Iconos
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { User, Mail, Phone, Calendar, History, PlusCircle, Edit, Loader2, Briefcase, AlertCircle } from "lucide-react";
import EditProfileModal from "../../components/EditProfileModal";

export default function Dashboard() {
  const { user, loading } = useAuth();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [citas, setCitas] = useState([]);
  const [loadingCitas, setLoadingCitas] = useState(true);
  const [errorCitas, setErrorCitas] = useState(null);

  if (!user) {
    return (
      <div className="flex justify-center items-center h-screen">
        <Loader2 className="h-12 w-12 animate-spin text-cyan-600" />
      </div>
    );
  }

  useEffect(() => {
    const fetchCitas = async () => {
      try {
        setLoadingCitas(true);
        const response = await apiClient.get('/citas/mis-citas');
        setCitas(response.data);
        setErrorCitas(null);
      } catch (error) {
        console.error("Error al obtener las citas:", error);
        setErrorCitas("No se pudieron cargar tus citas. Inténtalo de nuevo más tarde.");
      } finally {
        setLoadingCitas(false);
      }
    };

    fetchCitas();
  }, []);

  const ahora = new Date();
  const proximasCitas = citas.filter(cita => parseISO(cita.fechaHoraInicio) > ahora);
  const historialCitas = citas.filter(cita => parseISO(cita.fechaHoraInicio) <= ahora);

  // Componente para renderizar una tarjeta de cita individual
  const CitaCard = ({ cita }) => (
    <div className="border-l-4 border-cyan-500 pl-4 py-3 bg-gray-50 rounded-r-lg mb-4">
      <p className="font-bold text-gray-800">{cita.servicio.nombre}</p>
      <div className="text-sm text-gray-600 mt-1 space-y-1">
        <p className="flex items-center gap-2">
          <Calendar size={14} /> 
          {format(parseISO(cita.fechaHoraInicio), "EEEE, d 'de' MMMM 'de' yyyy 'a las' HH:mm", { locale: es })}h
        </p>
        <p className="flex items-center gap-2">
          <Briefcase size={14} />
          Con {cita.trabajador.nombre}
        </p>
      </div>
    </div>
  );


  return (
    <>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="container mx-auto py-8 px-4"
      >
        <div className="mb-8">
          <h1 className="text-3xl md:text-4xl font-bold text-gray-800">
            Bienvenida de nuevo, <span className="text-cyan-600">{user.nombre}</span>!
          </h1>
          <p className="text-lg text-gray-500 mt-2">Aquí tienes un resumen de tu actividad en SindiNails.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Columna Izquierda - Perfil */}
          <motion.div 
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="lg:col-span-1 space-y-8"
          >
            <Card className="shadow-lg hover:shadow-cyan-100 transition-shadow duration-300">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-xl"><User /> Mi Perfil</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center gap-3">
                  <User className="text-gray-400" size={20} />
                  <p className="text-gray-700">{user.nombre} {user.apellidos}</p>
                </div>
                <div className="flex items-center gap-3">
                  <Mail className="text-gray-400" size={20} />
                  <p className="text-gray-700">{user.email}</p>
                </div>
                <div className="flex items-center gap-3">
                  <Phone className="text-gray-400" size={20} />
                  <p className="text-gray-700">{user.telefono}</p>
                </div>
              </CardContent>
              <CardFooter>
                <Button 
                  variant="outline" 
                  className="w-full border-cyan-600 text-cyan-600 hover:bg-cyan-600 hover:text-white"
                  onClick={() => setIsModalOpen(true)}
                >
                  <Edit className="mr-2 h-4 w-4" /> Editar Perfil
                </Button>
              </CardFooter>
            </Card>
          </motion.div>

          {/* Columna Derecha - Citas y Acciones */}
          <motion.div 
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.4 }}
            className="lg:col-span-2 space-y-8"
          >
            <Card className="bg-cyan-600 text-white shadow-xl">
              <CardHeader>
                <CardTitle className="text-2xl">¿Lista para tu próxima cita?</CardTitle>
                <CardDescription className="text-cyan-100">Explora nuestros tratamientos y encuentra tu momento de relax.</CardDescription>
              </CardHeader>
              <CardFooter>
                <Button asChild size="lg" variant="secondary" className="bg-white text-cyan-600 hover:bg-gray-100">
                  <Link to="/servicios"><PlusCircle className="mr-2 h-5 w-5" /> Reservar Nueva Cita</Link>
                </Button>
              </CardFooter>
            </Card>

            <Card className="shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center gap-2"><Calendar /> Próximas Citas</CardTitle>
              </CardHeader>
              <CardContent>
                {loadingCitas ? (
                  <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>
                ) : errorCitas ? (
                  <div className="text-center py-10 text-red-500 flex flex-col items-center gap-2"><AlertCircle size={20} /> <p>{errorCitas}</p></div>
                ) : proximasCitas.length > 0 ? (
                  proximasCitas.map(cita => <CitaCard key={cita.id} cita={cita} />)
                ) : (
                  <div className="text-center py-10 text-gray-500"><p>Aún no tienes citas programadas.</p></div>
                )}
              </CardContent>
            </Card>

            <Card className="shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center gap-2"><History /> Historial de Citas</CardTitle>
              </CardHeader>
              <CardContent>
                {loadingCitas ? (
                  <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>
                ) : errorCitas ? (
                  <div className="text-center py-10 text-red-500 flex flex-col items-center gap-2"><AlertCircle size={20} /> <p>{errorCitas}</p></div>
                ) : historialCitas.length > 0 ? (
                  historialCitas.map(cita => <CitaCard key={cita.id} cita={cita} />)
                ) : (
                  <div className="text-center py-10 text-gray-500"><p>Aquí aparecerán tus citas completadas.</p></div>
                )}
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </motion.div>

      <EditProfileModal isOpen={isModalOpen} onOpenChange={setIsModalOpen} />
    </>
  );
}