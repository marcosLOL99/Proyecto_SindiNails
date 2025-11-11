import { Outlet } from 'react-router-dom';
import './App.css'
import Navbar from './components/Navbar'; // Importa el nuevo componente Navbar

function App() {
  return (
    <>
      <Navbar /> {/* Renderiza el componente Navbar aquí */}
      <main className="pt-16"> {/* Añade padding top para evitar que el contenido quede oculto detrás del navbar */}
        <Outlet /> {/* Aquí se renderizarán los componentes de las rutas hijas */}
      </main>
    </>
  );
}

export default App;
