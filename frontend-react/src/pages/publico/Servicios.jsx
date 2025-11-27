import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { motion } from 'framer-motion';
import { Button } from "@/components/ui/button";
import { Loader2, Euro, Clock, PlusCircle } from 'lucide-react';

export default function Servicios() {
  const [servicios, setServicios] = useState([]);
  const [tiposServicios, setTiposServicios] = useState([]);
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
          apiClient.get('/servicios'),
          apiClient.get('/tiposervicios')
        ]);
        setServicios(serviciosRes.data);
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

  const handleScrollToType = (tipoId) => {
    document.getElementById(`tipo-${tipoId}`).scrollIntoView({ behavior: 'smooth' });
  };

  const handleBookingClick = (serviceId) => {
    if (isAuthenticated) {
      navigate(`/reservar/${serviceId}`);
    } else {
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
      <div className="space-y-12">
        {tiposServicios.map(tipo => {
          const serviciosDeEsteTipo = servicios.filter(s => s.tipoServicio.id === tipo.id);
          if (serviciosDeEsteTipo.length === 0) return null;

          return (
            <motion.div
              key={tipo.id}
              id={`tipo-${tipo.id}`}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.2 }}
              transition={{ duration: 0.5 }}
            >
              <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-200 pb-2">{tipo.nombre}</h2>
              <div className="divide-y divide-gray-200">
                {serviciosDeEsteTipo.map(service => (
                  <div key={service.id} className="flex flex-col md:flex-row justify-between items-start md:items-center py-6">
                    <div className="md:w-2/3 mb-4 md:mb-0">
                      <h3 className="text-xl font-semibold text-gray-800">{service.nombre}</h3>
                      <p className="text-gray-600 mt-1">{service.descripcion}</p>
                    </div>
                    <div className="w-full md:w-1/3 flex flex-row md:flex-col items-center md:items-end gap-4 md:gap-2">
                      <div className="flex items-center gap-4 text-gray-700">
                        <span className="font-bold text-lg text-cyan-600">{service.precio}€</span>
                        <span className="text-sm text-gray-500">{service.duracionEnMinutos} min.</span>
                      </div>
                      <Button onClick={() => handleBookingClick(service.id)} size="sm" className="bg-cyan-600 hover:bg-cyan-700 text-white whitespace-nowrap">
                        Reservar
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          );
        })}
      </div>
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
          <div className="flex flex-wrap justify-center gap-3 mb-16 sticky top-0 bg-gray-50/80 backdrop-blur-sm py-4 z-10">
            {tiposServicios.map(tipo => (
              <Button key={tipo.id} onClick={() => handleScrollToType(tipo.id)} variant="outline" className="border-cyan-600 text-cyan-600 hover:bg-cyan-600 hover:text-white transition-colors">
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