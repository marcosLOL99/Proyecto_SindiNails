import { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Loader2, Euro, Clock, PlusCircle } from 'lucide-react';

export default function Servicios() {
  const [servicios, setServicios] = useState([]);
  const [tiposServicios, setTiposServicios] = useState([]);
  const [filteredServicios, setFilteredServicios] = useState([]);
  const [selectedType, setSelectedType] = useState('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        // Hacemos las dos llamadas a la API en paralelo para más eficiencia
        const [serviciosRes, tiposRes] = await Promise.all([
          axios.get('http://localhost:8080/api/servicios'),
          axios.get('http://localhost:8080/api/tiposervicios')
        ]);

        setServicios(serviciosRes.data);
        setFilteredServicios(serviciosRes.data); // Inicialmente mostramos todos
        setTiposServicios(tiposRes.data);
        setError(null);
      } catch (error) {
        console.error("Hubo un error al obtener los datos:", error);
        setError("No se pudieron cargar los servicios. Por favor, inténtalo más tarde.");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Función para manejar el filtrado
  const handleFilter = (tipoId) => {
    setSelectedType(tipoId);
    if (tipoId === 'all') {
      setFilteredServicios(servicios);
    } else {
      setFilteredServicios(servicios.filter(s => s.tipoServicio.id === tipoId));
    }
  };

  const handleBookingClick = (serviceId) => {
    if (isAuthenticated) {
      // Si está autenticado, lo llevamos a la página de reserva (que crearemos después)
      navigate(`/reservar/${serviceId}`);
    } else {
      // Si no, lo mandamos a iniciar sesión
      navigate('/login');
    }
  };
  const renderContent = () => {
    if (loading) {
      return (
        <div className="flex justify-center items-center py-20">
          <Loader2 className="h-12 w-12 animate-spin text-cyan-600" />
          <p className="ml-4 text-lg text-gray-600">Cargando nuestros tratamientos...</p>
        </div>
      );
    }

    if (error) {
      return <p className="text-center text-red-500 py-20">{error}</p>;
    }

    return (
      <motion.div layout className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <AnimatePresence>
          {filteredServicios.map((service, index) => (
            <motion.div
              layout
              key={service.id}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              transition={{ duration: 0.3, delay: index * 0.05 }}
            >
              <Card className="bg-white border-gray-200 flex flex-col shadow-lg hover:shadow-cyan-100 transition-all duration-300 h-full">
                <CardHeader>
                  <CardTitle className="text-gray-900">{service.nombre}</CardTitle>
                  <CardDescription className="text-gray-600 pt-2 min-h-[60px]">{service.descripcion}</CardDescription>
                </CardHeader>
                <CardContent className="flex-grow">
                  <div className="w-full h-40 bg-gray-100 rounded-md flex items-center justify-center">
                    <span className="text-gray-400 text-sm">Imagen del Servicio</span>
                  </div>
                </CardContent>
                <CardFooter className="flex-col items-start gap-4">
                  <div className="w-full flex justify-between items-center text-gray-800">
                    <div className="flex items-center gap-2 text-cyan-600 font-bold text-lg">
                      <Euro size={20} />
                      <span>{service.precio}</span>
                    </div>
                    <div className="flex items-center gap-2 text-gray-500 text-sm">
                      <Clock size={16} />
                      <span>{service.duracionEnMinutos} min</span>
                    </div>
                  </div>
                  <Button onClick={() => handleBookingClick(service.id)} className="w-full bg-cyan-600 hover:bg-cyan-700">
                    <PlusCircle className="mr-2 h-4 w-4" /> Reservar ahora
                  </Button>
                </CardFooter>
              </Card>
            </motion.div>
          ))}
        </AnimatePresence>
      </motion.div>
    );
  };

  return (
    <div className="bg-gray-50 text-gray-800 py-12 md:py-20 px-4">
      <div className="container mx-auto">
        <div className="text-center mb-12">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-gray-900">
            Nuestros <span className="text-cyan-600">Servicios</span>
          </h1>
          <p className="mt-4 max-w-2xl mx-auto text-lg text-gray-600">
            Explora la gama completa de tratamientos que ofrecemos para realzar tu belleza.
          </p>
        </div>

        {/* Filtros */}
        {!loading && !error && (
          <div className="flex flex-wrap justify-center gap-2 mb-12">
            <Button onClick={() => handleFilter('all')} variant={selectedType === 'all' ? 'default' : 'outline'} className={selectedType === 'all' ? 'bg-cyan-600 hover:bg-cyan-700' : 'border-cyan-600 text-cyan-600 hover:bg-cyan-600 hover:text-white'}>Todos</Button>
            {tiposServicios.map(tipo => (
              <Button key={tipo.id} onClick={() => handleFilter(tipo.id)} variant={selectedType === tipo.id ? 'default' : 'outline'} className={selectedType === tipo.id ? 'bg-cyan-600 hover:bg-cyan-700' : 'border-cyan-600 text-cyan-600 hover:bg-cyan-600 hover:text-white'}>
                {tipo.nombre}
              </Button>
            ))}
          </div>
        )}

        {renderContent()}
      </div>
    </div>
  );
}