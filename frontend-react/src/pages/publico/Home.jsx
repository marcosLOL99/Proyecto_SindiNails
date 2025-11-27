import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Sparkles, Gem, ShieldCheck } from 'lucide-react';
import HeroCarousel from '../../components/HeroCarousel';
import Testimonios from '../../components/Testimonios';
import InstagramFeed from '../../components/InstagramFeed';

// Importamos las imágenes de los servicios
import imgServicio1 from '../../assets/imgServicio/servicio1.jpeg';
import imgServicio2 from '../../assets/imgServicio/servicio2.jpeg';
import imgServicio3 from '../../assets/imgServicio/servicio3.jpeg';
import imgServicio4 from '../../assets/imgServicio/servicio4.jpeg';

const servicioImages = [imgServicio1, imgServicio2, imgServicio3, imgServicio4];

export default function Home() {
  const [servicios, setServicios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    apiClient.get('/servicios')
      .then(response => {
        setServicios(response.data);
        setLoading(false);
      })
      .catch(error => {
        console.error("Hubo un error al obtener los servicios:", error);
        setError("No se pudieron cargar los servicios. ¿El backend está funcionando?");
        setLoading(false);
      });
  }, []);

  const renderServicios = () => {
    if (loading) {
      return <p className="text-center text-gray-400">Cargando nuestros tratamientos...</p>;
    }
    if (error) {
      return <p className="text-center text-red-500">{error}</p>;
    }
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
        {servicios.slice(0, 4).map((service, index) => (
          <motion.div
            key={service.id}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.5 }}
            transition={{ duration: 0.5 }}
          >
            {/* La tarjeta ahora es un contenedor flex que organiza su contenido verticalmente */}
            <Card className="bg-white border-gray-200 flex flex-col shadow-lg hover:shadow-xl transition-shadow duration-300 h-full overflow-hidden">
              {/* Contenedor de la imagen */}
              <div className="w-full h-40 overflow-hidden">
                <img
                  src={servicioImages[index]}
                  alt={service.nombre}
                  className="w-full h-full object-cover transition-transform duration-300 hover:scale-110"
                />
              </div>
              {/* El contenido de texto ahora crece para empujar el footer hacia abajo */}
              <CardContent className="p-4 flex-grow flex flex-col">
                <CardTitle className="text-gray-900 text-lg mb-2">{service.nombre}</CardTitle>
                <CardDescription className="text-gray-600 text-sm flex-grow">{service.descripcion}</CardDescription>
              </CardContent>
              <CardFooter className="p-4 pt-0 flex justify-between items-center bg-white">
                <p className="text-cyan-600 font-bold text-lg">{service.precio}€</p>
                <p className="text-gray-500 text-sm">{service.duracionEnMinutos} min</p>
              </CardFooter>
            </Card>
          </motion.div>
        ))}
      </div>
    );
  };

  return (
    <div className="bg-white text-gray-800">
      {/* --- Hero Section --- */}
      <section className="relative min-h-screen flex items-center justify-center text-center px-4 overflow-hidden">
        {/* El carrusel como fondo absoluto */}
        <div className="absolute inset-0 z-0">
          <HeroCarousel />
        </div>
        {/* Un overlay oscuro para mejorar la legibilidad del texto */}
        <div className="absolute inset-0 bg-black/50 z-0"></div>
        <motion.div
          className="relative z-10"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: "easeInOut" }}
        >
          <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-4 text-white">
            Sindi Nails <span className="text-cyan-400">Beauty Studio</span>
          </h1>
          <p className="max-w-2xl mx-auto text-lg md:text-xl text-gray-200 mb-8">
            Donde el arte y la belleza se encuentran en tus manos. Especialistas en diseños únicos y cuidado profesional.
          </p>
          <motion.div
            className="flex flex-col sm:flex-row items-center justify-center gap-4"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <Button asChild size="lg" className="bg-cyan-600 hover:bg-cyan-700 text-white font-bold w-full sm:w-auto">
              <Link to="/servicios">Reservar Cita</Link>
            </Button>
            <Button asChild size="lg" variant="outline" className="text-white border-white hover:bg-white hover:text-cyan-700 w-full sm:w-auto">
              <Link to="/servicios">Ver Tratamientos</Link>
            </Button>
          </motion.div>
        </motion.div>
      </section>

      {/* --- Features Section --- */}
      <motion.section
        className="py-20 bg-gray-50 px-4"
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.2 }}
        transition={{ duration: 0.5 }}
      >
        <div className="container mx-auto grid md:grid-cols-3 gap-12 text-center">
          <div className="flex flex-col items-center">
            <Gem size={40} className="text-cyan-600 mb-4" />
            <h3 className="text-xl font-bold mb-2 text-gray-900">Materiales de Calidad</h3>
            <p className="text-gray-400">Usamos solo productos de alta gama para garantizar un acabado perfecto y duradero.</p>
          </div>
          <div className="flex flex-col items-center">
            <Sparkles size={40} className="text-cyan-600 mb-4" />
            <h3 className="text-xl font-bold mb-2 text-gray-900">Diseños Creativos</h3>
            <p className="text-gray-400">Desde lo más clásico a las últimas tendencias. Tu imaginación es nuestro límite.</p>
          </div>
          <div className="flex flex-col items-center">
            <ShieldCheck size={40} className="text-cyan-600 mb-4" />
            <h3 className="text-xl font-bold mb-2 text-gray-900">Higiene Garantizada</h3>
            <p className="text-gray-400">Esterilizamos nuestro material para cada cliente. Tu seguridad es nuestra prioridad.</p>
          </div>
        </div>
      </motion.section>

      {/* --- Servicios Destacados Section --- */}
      <motion.section
        className="py-20 bg-white px-4"
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.2 }}
        transition={{ duration: 0.5 }}
      >
        <div className="container mx-auto">
          <h2 className="text-4xl font-bold text-center mb-12 text-gray-900">Tratamientos Estrella</h2>
          {renderServicios()}
          <div className="text-center mt-12">
            <Button asChild variant="outline" className="border-cyan-600 text-cyan-600 hover:bg-cyan-600 hover:text-white transition-colors">
              <Link to="/servicios">Ver todos los servicios</Link>
            </Button>
          </div>
        </div>
      </motion.section>

      {/* --- Testimonials Section --- */}
      <motion.section
        className="py-20 bg-gray-50 px-4"
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.2 }}
        transition={{ duration: 0.5 }}
      >
        <div className="container mx-auto">
          <h2 className="text-4xl font-bold text-center mb-4 text-gray-900">Lo que dicen nuestras clientas</h2>
          <p className="text-center text-gray-500 mb-12">Tu satisfacción es nuestra mejor publicidad.</p>
          <Testimonios />
        </div>
      </motion.section>

      {/* --- Instagram Feed Section --- */}
      <motion.section
        className="py-20 bg-white px-4"
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.2 }}
        transition={{ duration: 0.5 }}
      >
        <div className="container mx-auto">
          <h2 className="text-4xl font-bold text-center mb-4 text-gray-900">
            Síguenos en <span className="text-cyan-600">Instagram</span>
          </h2>
          <p className="text-center text-gray-500 mb-12">Descubre nuestros últimos trabajos y promociones.</p>
          <InstagramFeed />
        </div>
      </motion.section>


      {/* --- CTA Section --- */}
      <motion.section
        className="py-20 bg-gray-50 text-center px-4"
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.2 }}
        transition={{ duration: 0.5 }}
      >
        <div className="container mx-auto">
          <h2 className="text-3xl font-bold mb-4 text-gray-900">¿Lista para unas uñas espectaculares?</h2>
          <p className="text-gray-400 mb-8">Pide tu cita hoy mismo y déjanos cuidar de ti.</p>
          <Button asChild size="lg" className="bg-cyan-600 hover:bg-cyan-700 text-white font-bold">
            <Link to="/contacto">Contactar Ahora</Link>
          </Button>
        </div>
      </motion.section>
    </div>
  );
}