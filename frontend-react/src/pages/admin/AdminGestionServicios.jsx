import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from '@/components/ui/button';
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PlusCircle, Edit, Trash2, Loader2, FilterX } from 'lucide-react';
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
import { toast } from 'sonner';
import ServicioForm from '../../components/ServicioForm.jsx';

const AdminGestionServicios = () => {
  const [servicios, setServicios] = useState([]);
  const [filteredServicios, setFilteredServicios] = useState([]);
  const [tiposServicio, setTiposServicio] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [nombreFiltro, setNombreFiltro] = useState('');
  const [tipoServicioFiltro, setTipoServicioFiltro] = useState('');

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingServicio, setEditingServicio] = useState(null);
  const [deletingServicioId, setDeletingServicioId] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [serviciosRes, tiposServicioRes] = await Promise.all([
        apiClient.get('/servicios'),
        apiClient.get('/tiposervicios')
      ]);
      setServicios(serviciosRes.data);
      setFilteredServicios(serviciosRes.data);
      setTiposServicio(tiposServicioRes.data);
      setError(null);
    } catch (err) {
      setError('No se pudieron cargar los servicios.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    let result = servicios;
    if (nombreFiltro) {
      result = result.filter(s => s.nombre.toLowerCase().includes(nombreFiltro.toLowerCase()));
    }
    if (tipoServicioFiltro) {
      result = result.filter(s => s.tipoServicio.id === parseInt(tipoServicioFiltro));
    }
    setFilteredServicios(result);
  }, [nombreFiltro, tipoServicioFiltro, servicios]);

  const handleSave = () => {
    setIsFormOpen(false);
    setEditingServicio(null);
    fetchData();
  };

  const handleOpenForm = (servicio = null) => {
    setEditingServicio(servicio);
    setIsFormOpen(true);
  };

  const handleDelete = async () => {
    if (!deletingServicioId) return;
    try {
      await apiClient.delete(`/admin/servicios/${deletingServicioId}`);
      toast.success('Servicio eliminado con éxito');
      setDeletingServicioId(null);
      fetchData();
    } catch (error) {
      toast.error('No se pudo eliminar el servicio.');
      setDeletingServicioId(null);
    }
  };

  if (loading) return <div className="flex justify-center items-center h-64"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>;
  if (error) return <div className="text-center text-red-500 bg-red-100 p-4 rounded-md">{error}</div>;

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Gestión de Servicios</h1>
        <Button onClick={() => handleOpenForm()} className="bg-cyan-600 hover:bg-cyan-700 text-white flex items-center gap-2"><PlusCircle size={20} />Añadir Servicio</Button>
      </div>

      {/* Sección de Filtros */}
      <div className="flex items-center gap-4 mb-6 p-4 bg-gray-50 rounded-lg border">
        <Input
          placeholder="Filtrar por nombre..."
          value={nombreFiltro}
          onChange={(e) => setNombreFiltro(e.target.value)}
          className="max-w-sm"
        />
        <Select value={tipoServicioFiltro} onValueChange={setTipoServicioFiltro}>
          <SelectTrigger className="w-[280px]">
            <SelectValue placeholder="Filtrar por tipo..." />
          </SelectTrigger>
          <SelectContent className="bg-white">
            {tiposServicio.map(t => <SelectItem key={t.id} value={String(t.id)}>{t.nombre}</SelectItem>)}
          </SelectContent>
        </Select>
        <Button variant="ghost" onClick={() => { setNombreFiltro(''); setTipoServicioFiltro(''); }} className="flex items-center gap-2 text-gray-600">
          <FilterX size={16} /> Limpiar Filtros
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <Table>
          <TableHeader className="bg-gray-50">
            <TableRow>
              <TableHead className="font-bold">Nombre</TableHead>
              <TableHead className="font-bold">Precio</TableHead>
              <TableHead className="font-bold">Duración</TableHead>
              <TableHead className="font-bold text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredServicios.length > 0 ? (
              filteredServicios.map((servicio) => (
                <TableRow key={servicio.id}>
                <TableCell>{servicio.nombre}</TableCell>
                <TableCell>{servicio.precio.toFixed(2)} €</TableCell>
                <TableCell>{servicio.duracionEnMinutos} min</TableCell>
                <TableCell className="text-right">
                  <Button onClick={() => handleOpenForm(servicio)} variant="ghost" size="icon" className="text-blue-600 hover:text-blue-800"><Edit size={18} /></Button>
                  <Button onClick={() => setDeletingServicioId(servicio.id)} variant="ghost" size="icon" className="text-red-600 hover:text-red-800"><Trash2 size={18} /></Button>
                </TableCell>
              </TableRow>
              ))
            ) : (
              <TableRow><TableCell colSpan="4" className="text-center">No se encontraron servicios con los filtros aplicados.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-[425px] bg-white">
          <DialogHeader>
            <DialogTitle>{editingServicio ? 'Editar Servicio' : 'Añadir Servicio'}</DialogTitle>
          </DialogHeader>
          <ServicioForm servicio={editingServicio} onSave={handleSave} onCancel={() => setIsFormOpen(false)} />
        </DialogContent>
      </Dialog>
      <AlertDialog open={!!deletingServicioId} onOpenChange={() => setDeletingServicioId(null)}>
        <AlertDialogContent className="bg-white">
          <AlertDialogHeader>
            <AlertDialogTitle>¿Estás seguro?</AlertDialogTitle>
            <AlertDialogDescription>Esta acción eliminará permanentemente el servicio.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel><AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">Eliminar</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent></AlertDialog>
    </div>
  );
};

export default AdminGestionServicios;
