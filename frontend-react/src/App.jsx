import { Outlet } from 'react-router-dom';
import Navbar from './components/Navbar';
import { Toaster } from "@/components/ui/sonner"

function App() {
  return (
    <>
      <Navbar />
      <main className="pt-16">
        <Toaster richColors position="top-center" />
        <Outlet />
      </main>
    </>
  );
}

export default App;
