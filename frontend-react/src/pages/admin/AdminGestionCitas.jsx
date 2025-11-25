import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../../router/api';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Calendar } from "@/components/ui/calendar";
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
import { PlusCircle, Edit, Trash2, Loader2, CalendarIcon, FilterX } from 'lucide-react';
import CitaForm from '../../components/CitaForm';
import { toast } from 'sonner';

const AdminGestionCitas = () => {
  const [citas, setCitas] = useState([]);
  const [filteredCitas, setFilteredCitas] = useState([]);
  const [trabajadores, setTrabajadores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Estados para los filtros
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedTrabajador, setSelectedTrabajador] = useState('');

  // Estados para modales
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCita, setEditingCita] = useState(null);
  const [deletingCitaId, setDeletingCitaId] = useState(null);

  // useEffect para aplicar los filtros cuando cambian
  useEffect(() => {
    let result = citas;
    if (selectedDate) {
      result = result.filter(cita => format(new Date(cita.fechaHoraInicio), 'yyyy-MM-dd') === format(selectedDate, 'yyyy-MM-dd'));
    }
    if (selectedTrabajador) {
      result = result.filter(cita => cita.trabajador.id === parseInt(selectedTrabajador));
    }
    setFilteredCitas(result);
  }, [selectedDate, selectedTrabajador, citas]);
 
  const formatDate = (dateString) => {
    const options = { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString('es-ES', options);
  };

  // Definición de fetchData como useCallback para que sea estable y reutilizable
  const fetchData = useCallback(async (showToast = false) => {
    try {
      setLoading(true);
      const [citasResponse, trabajadoresResponse] = await Promise.all([
        apiClient.get('/admin/citas'),
        apiClient.get('/admin/trabajadores')
      ]);
      setCitas(citasResponse.data);
      setFilteredCitas(citasResponse.data);
      setTrabajadores(trabajadoresResponse.data);
      setError(null);
      if (showToast) {
        toast.success("Lista de citas actualizada.");
      }
    } catch (err) {
      setError('No se pudieron cargar los datos. Por favor, inténtalo de nuevo más tarde.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []); // Dependencias vacías si no usa props/estados externos que cambien

  const handleSave = useCallback(() => {
    setIsFormOpen(false);
    setEditingCita(null);
    fetchData(true); // Recarga los datos y muestra un toast
  }, [fetchData]);

  const handleOpenForm = useCallback((cita = null) => {
    setEditingCita(cita);
    setIsFormOpen(true);
  }, []);

  const handleDelete = useCallback(async () => {
    if (!deletingCitaId) return;
    try {
      await apiClient.delete(`/admin/citas/${deletingCitaId}`);
      toast.success('Cita eliminada con éxito');
      setDeletingCitaId(null);
      fetchData(); // Recarga los datos
    } catch (error) {
      toast.error('No se pudo eliminar la cita.');
      console.error(error);
      setDeletingCitaId(null);
    }
  }, [deletingCitaId, fetchData]);

  useEffect(() => {
    fetchData();
  }, [fetchData]); // Ahora fetchData es una dependencia estable

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 bg-red-100 p-4 rounded-md">{error}</div>;
  }

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Gestión de Citas</h1>
        <Button onClick={() => handleOpenForm()} className="bg-cyan-600 hover:bg-cyan-700 text-white flex items-center gap-2">
          <PlusCircle size={20} />
          Añadir Cita
        </Button>
      </div>

      {/* Sección de Filtros */}
      <div className="flex items-center gap-4 mb-6 p-4 bg-gray-50 rounded-lg border">
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant={"outline"}
              className={`w-[280px] justify-start text-left font-normal ${!selectedDate && "text-muted-foreground"}`}
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {selectedDate ? format(selectedDate, "PPP", { locale: es }) : <span>Selecciona una fecha</span>}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0 bg-white">
            <Calendar
              mode="single"
              selected={selectedDate}
              onSelect={setSelectedDate}
              initialFocus
            />
          </PopoverContent>
        </Popover>

        <Select value={selectedTrabajador} onValueChange={setSelectedTrabajador}>
          <SelectTrigger className="w-[280px]">
            <SelectValue placeholder="Filtrar por trabajador..." />
          </SelectTrigger>
          <SelectContent className="w-[280px] bg-white">
            {trabajadores.map(t => <SelectItem key={t.id} value={String(t.id)}>{t.nombre} {t.apellidos}</SelectItem>)}
          </SelectContent>
        </Select>

        <Button variant="ghost" onClick={() => { setSelectedDate(null); setSelectedTrabajador(''); }} className="flex items-center gap-2 text-gray-600">
          <FilterX size={16} /> Limpiar Filtros
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <Table>
          <TableHeader className="bg-gray-50">
            <TableRow>
              <TableHead className="font-bold">Cliente</TableHead>
              <TableHead className="font-bold">Servicio</TableHead>
              <TableHead className="font-bold">Trabajador</TableHead>
              <TableHead className="font-bold">Fecha y Hora</TableHead>
              <TableHead className="font-bold text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredCitas.length > 0 ? (
              filteredCitas.map((cita) => (
                <TableRow key={cita.id}>
                  <TableCell>{`${cita.cliente.nombre} ${cita.cliente.apellidos}`}</TableCell>
                  <TableCell>{cita.servicio.nombre}</TableCell>
                  <TableCell>{cita.trabajador.nombre}</TableCell>
                  <TableCell>{formatDate(cita.fechaHoraInicio)}</TableCell>
                  <TableCell className="text-right">
                    <Button onClick={() => handleOpenForm(cita)} variant="ghost" size="icon" className="text-blue-600 hover:text-blue-800">
                      <Edit size={18} />
                    </Button>
                    <Button onClick={() => setDeletingCitaId(cita.id)} variant="ghost" size="icon" className="text-red-600 hover:text-red-800">
                      <Trash2 size={18} />
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow><TableCell colSpan="5" className="text-center">No hay citas para mostrar.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* Modal para Añadir/Editar Cita */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-[425px] bg-white">
          <DialogHeader>
            <DialogTitle>{editingCita ? 'Editar Cita' : 'Añadir Nueva Cita'}</DialogTitle>
          </DialogHeader>
          <CitaForm cita={editingCita} onSave={handleSave} onCancel={() => setIsFormOpen(false)} />
        </DialogContent>
      </Dialog>

      {/* Diálogo de Confirmación para Eliminar */}
      <AlertDialog open={!!deletingCitaId} onOpenChange={() => setDeletingCitaId(null)}>
        <AlertDialogContent className="bg-white">
          <AlertDialogHeader>
            <AlertDialogTitle>¿Estás absolutamente seguro?</AlertDialogTitle>
            <AlertDialogDescription>Esta acción no se puede deshacer. Esto eliminará permanentemente la cita.</AlertDialogDescription>
          </AlertDialogHeader> 
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">Eliminar</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};

export default AdminGestionCitas;
