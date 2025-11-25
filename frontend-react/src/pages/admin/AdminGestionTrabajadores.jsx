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
import TrabajadorForm from '../../components/TrabajadorForm.jsx';

const AdminGestionTrabajadores = () => {
  const [trabajadores, setTrabajadores] = useState([]);
  const [filteredTrabajadores, setFilteredTrabajadores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Estado para el filtro
  const [nombreFiltro, setNombreFiltro] = useState('');

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingTrabajador, setEditingTrabajador] = useState(null);
  const [deletingTrabajadorId, setDeletingTrabajadorId] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get('/admin/trabajadores');
      setTrabajadores(response.data);
      setFilteredTrabajadores(response.data);
      setError(null);
    } catch (err) {
      setError('No se pudieron cargar los trabajadores.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // useEffect para aplicar el filtro por nombre
  useEffect(() => {
    let result = trabajadores;
    if (nombreFiltro) {
      result = result.filter(t => t.nombre.toLowerCase().includes(nombreFiltro.toLowerCase()) || t.apellidos.toLowerCase().includes(nombreFiltro.toLowerCase()));
    }
    setFilteredTrabajadores(result);
  }, [nombreFiltro, trabajadores]);

  const handleSave = () => {
    setIsFormOpen(false);
    setEditingTrabajador(null);
    fetchData();
  };

  const handleOpenForm = (trabajador = null) => {
    setEditingTrabajador(trabajador);
    setIsFormOpen(true);
  };

  const handleDelete = async () => {
    if (!deletingTrabajadorId) return;
    try {
      await apiClient.delete(`/admin/trabajadores/${deletingTrabajadorId}`);
      toast.success('Trabajador eliminado con éxito');
      setDeletingTrabajadorId(null);
      fetchData();
    } catch (error) {
      toast.error('No se pudo eliminar el trabajador.');
      setDeletingTrabajadorId(null);
    }
  };

  if (loading) return <div className="flex justify-center items-center h-64"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>;
  if (error) return <div className="text-center text-red-500 bg-red-100 p-4 rounded-md">{error}</div>;

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Gestión de Trabajadores</h1>
        <Button onClick={() => handleOpenForm()} className="bg-cyan-600 hover:bg-cyan-700 text-white flex items-center gap-2"><PlusCircle size={20} />Añadir Trabajador</Button>
      </div>

      {/* Sección de Filtros */}
      <div className="flex items-center gap-4 mb-6 p-4 bg-gray-50 rounded-lg border">
        <Input
          placeholder="Filtrar por nombre o apellidos..."
          value={nombreFiltro}
          onChange={(e) => setNombreFiltro(e.target.value)}
          className="max-w-sm"
        />
        <Button variant="ghost" onClick={() => setNombreFiltro('')} className="flex items-center gap-2 text-gray-600">
          <FilterX size={16} /> Limpiar Filtro
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <Table>
          <TableHeader className="bg-gray-50">
            <TableRow>
              <TableHead className="font-bold">Nombre</TableHead>
              <TableHead className="font-bold">Apellidos</TableHead>
              <TableHead className="font-bold">Estado</TableHead>
              <TableHead className="font-bold text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredTrabajadores.length > 0 ? (
              filteredTrabajadores.map((trabajador) => (
                <TableRow 
                  key={trabajador.id}
                  className={!trabajador.activo ? 'bg-gray-100 text-gray-500' : ''}
                >
                  <TableCell>{trabajador.nombre}</TableCell>
                  <TableCell>{trabajador.apellidos}</TableCell>
                  <TableCell>
                    {trabajador.activo ? (
                      <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">
                        Activo
                      </span>
                    ) : (
                      <span className="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">
                        Inactivo
                      </span>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button onClick={() => handleOpenForm(trabajador)} variant="ghost" size="icon" className="text-blue-600 hover:text-blue-800"><Edit size={18} /></Button>
                    <Button onClick={() => setDeletingTrabajadorId(trabajador.id)} variant="ghost" size="icon" className="text-red-600 hover:text-red-800"><Trash2 size={18} /></Button>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow><TableCell colSpan="4" className="text-center">No se encontraron trabajadores con los filtros aplicados.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-[425px] bg-white">
          <DialogHeader>
            <DialogTitle>{editingTrabajador ? 'Editar Trabajador' : 'Añadir Trabajador'}</DialogTitle>
          </DialogHeader>
          <TrabajadorForm trabajador={editingTrabajador} onSave={handleSave} onCancel={() => setIsFormOpen(false)} />
        </DialogContent>
      </Dialog>
      <AlertDialog open={!!deletingTrabajadorId} onOpenChange={() => setDeletingTrabajadorId(null)}>
        <AlertDialogContent className="bg-white">
          <AlertDialogHeader>
            <AlertDialogTitle>¿Estás seguro?</AlertDialogTitle>
            <AlertDialogDescription>Esta acción eliminará permanentemente al trabajador.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel><AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">Eliminar</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent></AlertDialog>
    </div>
  );
};

export default AdminGestionTrabajadores;
