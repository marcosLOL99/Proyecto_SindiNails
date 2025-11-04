import { useState, useEffect } from 'react'
import axios from 'axios';
import './App.css'

function App() {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get('http://localhost:8080/api/services')
      .then(response => {
        setServices(response.data);
        setLoading(false);
      })
      .catch(error => {
        console.error("Hubo un error al obtener los servicios:", error);
        setError("No se pudieron cargar los servicios. ¿El backend está funcionando?");
        setLoading(false);
      });
  }, []); // El array vacío asegura que esto se ejecute solo una vez, al montar el componente.

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-pink-100">
      <div className="text-center p-8">
        <h1 className="text-4xl font-bold text-pink-600">Centro de Estética SindiNails</h1>
        <p className="text-gray-600 mt-2 mb-8">Bienvenida a nuestra web 💅</p>

        <h2 className="text-2xl font-semibold text-pink-500 mb-4">Nuestros Servicios</h2>
        {loading && <p>Cargando servicios...</p>}
        {error && <p className="text-red-500">{error}</p>}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {services.map(service => (
            <div key={service.id} className="bg-white p-4 rounded-lg shadow-md text-left">
              <h3 className="text-xl font-bold text-gray-800">{service.name}</h3>
              <p className="text-gray-600">{service.description}</p>
              <p className="text-pink-500 font-semibold mt-2">Precio: {service.price}€</p>
              <p className="text-gray-500 text-sm">Duración: {service.durationInMinutes} min</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
