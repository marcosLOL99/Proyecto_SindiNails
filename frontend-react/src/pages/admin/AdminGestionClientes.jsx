import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from '@/components/ui/button';
import { Input } from "@/components/ui/input";
import { Loader2, Mail, Phone, Edit, Trash2, Users, PlusCircle, FilterX } from 'lucide-react';
import ClienteForm from '../../components/ClienteForm';
import { toast } from 'sonner';

// Componente para el avatar con las iniciales
const Avatar = ({ nombre, apellidos }) => {
  const iniciales = `${nombre?.[0] || ''}${apellidos?.[0] || ''}`.toUpperCase();
  return (
    <div className="w-12 h-12 rounded-full bg-cyan-500 text-white flex items-center justify-center text-xl font-bold flex-shrink-0">
      {iniciales}
    </div>
  );
};

const AdminGestionClientes = () => {
  const [clientes, setClientes] = useState([]);
  const [filteredClientes, setFilteredClientes] = useState([]);
  const [filtro, setFiltro] = useState('');
  const [selectedCliente, setSelectedCliente] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCliente, setEditingCliente] = useState(null);
  const [deletingClienteId, setDeletingClienteId] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
      try {
        setLoading(true);
        const response = await apiClient.get('/admin/clientes');
        // Ordenar alfabéticamente por nombre
        const sortedClientes = response.data.sort((a, b) => a.nombre.localeCompare(b.nombre));
        setClientes(sortedClientes);
        setFilteredClientes(sortedClientes);
        setError(null);
      } catch (err) {
        setError('No se pudieron cargar los clientes.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
  
  // useEffect para aplicar el filtro
  useEffect(() => {
    let result = clientes;
    if (filtro) {
      const lowercasedFilter = filtro.toLowerCase();
      result = clientes.filter(cliente =>
        cliente.nombre.toLowerCase().includes(lowercasedFilter) ||
        cliente.apellidos.toLowerCase().includes(lowercasedFilter) ||
        cliente.email.toLowerCase().includes(lowercasedFilter)
      );
    }
    setFilteredClientes(result);
  }, [filtro, clientes]);
  
  const handleSave = () => {
    setIsFormOpen(false);
    setEditingCliente(null);
    fetchData();
    // Si el cliente seleccionado fue el que se editó, lo refrescamos
    if (selectedCliente && editingCliente && selectedCliente.id === editingCliente.id) {
      setSelectedCliente(null); // Forzamos a que se refresque la vista de detalles
    }
  };

  const handleOpenForm = (cliente = null) => {
    setEditingCliente(cliente);
    setIsFormOpen(true);
  };

  const handleDelete = async () => {
    if (!deletingClienteId) return;
    try {
      await apiClient.delete(`/admin/clientes/${deletingClienteId}`);
      toast.success('Cliente desactivado con éxito');
      setDeletingClienteId(null);
      setSelectedCliente(null); // Deseleccionar para evitar mostrar datos de un cliente "borrado"
      fetchData();
    } catch (error) {
      toast.error('No se pudo desactivar el cliente.');
      setDeletingClienteId(null);
    }
  };

  if (loading) return <div className="flex justify-center items-center h-64"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>;
  if (error) return <div className="text-center text-red-500 bg-red-100 p-4 rounded-md">{error}</div>;

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Gestión de Clientes</h1>
        <Button onClick={() => handleOpenForm()} className="bg-cyan-600 hover:bg-cyan-700 text-white flex items-center gap-2"><PlusCircle size={20} />Añadir Cliente</Button>
      </div>

      {/* Sección de Filtros */}
      <div className="flex items-center gap-4 mb-6 p-4 bg-gray-50 rounded-lg border">
        <Input
          placeholder="Filtrar por nombre, apellidos o email..."
          value={filtro}
          onChange={(e) => setFiltro(e.target.value)}
          className="max-w-sm"
        />
        <Button variant="ghost" onClick={() => setFiltro('')} className="flex items-center gap-2 text-gray-600">
          <FilterX size={16} /> Limpiar Filtro
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Columna de la Lista de Clientes */}
        <Card className="md:col-span-1">
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Users /> Clientes ({filteredClientes.length})</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="max-h-[60vh] overflow-y-auto pr-2">
              <ul className="space-y-2">
                {filteredClientes.map(cliente => (
                  <li key={cliente.id} onClick={() => setSelectedCliente(cliente)}
                      className={`flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-colors ${selectedCliente?.id === cliente.id ? 'bg-cyan-100' : 'hover:bg-gray-100'}
                      `}>
                    <Avatar nombre={cliente.nombre} apellidos={cliente.apellidos} />
                    <div>
                      <p className="font-semibold text-gray-800">{cliente.nombre} {cliente.apellidos}</p>
                      <p className="text-sm text-gray-500">{cliente.email}</p>
                    </div>
                  </li>
                ))}
                {filteredClientes.length === 0 && (
                  <li className="text-center text-gray-500 py-4">No se encontraron clientes.</li>
                )}
              </ul>
            </div>
          </CardContent>
        </Card>

        {/* Columna de Detalles del Cliente */}
        <div className="md:col-span-2">
          {selectedCliente ? (
            <Card className="shadow-lg">
              <CardHeader>
                <div className="flex items-center gap-4">
                  <Avatar nombre={selectedCliente.nombre} apellidos={selectedCliente.apellidos} />
                  <div>
                    <CardTitle className="text-2xl">{selectedCliente.nombre} {selectedCliente.apellidos}</CardTitle>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4 pt-2">
                
                <div className="flex items-center gap-3"><Mail className="text-gray-400" size={20} /><p>{selectedCliente.email}</p></div>
                <div className="flex items-center gap-3"><Phone className="text-gray-400" size={20} /><p>{selectedCliente.telefono || 'No especificado'}</p></div>
                <div className="flex gap-1">
                  <Button onClick={() => handleOpenForm(selectedCliente)} variant="ghost" size="icon" className="text-blue-600 hover:text-blue-800 h-8 w-8"><Edit size={16} /></Button>
                  <Button onClick={() => setDeletingClienteId(selectedCliente.id)} variant="ghost" size="icon" className="text-red-600 hover:text-red-800 h-8 w-8"><Trash2 size={16} /></Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <div className="flex items-center justify-center h-full bg-gray-50 rounded-lg border-2 border-dashed">
              <p className="text-gray-500">Selecciona un cliente para ver sus detalles</p>
            </div>
          )}
        </div>
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-[425px] bg-white">
          <DialogHeader>
            <DialogTitle>{editingCliente ? 'Editar Cliente' : 'Añadir Cliente'}</DialogTitle>
          </DialogHeader>
          <ClienteForm cliente={editingCliente} onSave={handleSave} onCancel={() => setIsFormOpen(false)} />
        </DialogContent>
      </Dialog>

      <AlertDialog open={!!deletingClienteId} onOpenChange={() => setDeletingClienteId(null)}>
        <AlertDialogContent className="bg-white">
          <AlertDialogHeader>
            <AlertDialogTitle>¿Estás seguro?</AlertDialogTitle>
            <AlertDialogDescription>Esta acción marcará al cliente como inactivo. No se eliminará permanentemente.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel><AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">Desactivar</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};

export default AdminGestionClientes;
