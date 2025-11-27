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
import { PlusCircle, Edit, Trash2, Loader2, FilterX, CalendarOff, X } from 'lucide-react';
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
import FormularioAusencia from '../../components/FormularioAusencia.jsx';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

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
  const [isAusenciaFormOpen, setIsAusenciaFormOpen] = useState(false);
  const [selectedTrabajadorAusencias, setSelectedTrabajadorAusencias] = useState(null);
  const [ausencias, setAusencias] = useState([]);
  const [isFestivoFormOpen, setIsFestivoFormOpen] = useState(false);
  const [festivosGlobales, setFestivosGlobales] = useState([]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [trabajadoresRes, festivosRes] = await Promise.all([
        apiClient.get('/admin/trabajadores'),
        apiClient.get('/admin/bloqueos/globales')
      ]);
      setTrabajadores(trabajadoresRes.data);
      setFilteredTrabajadores(trabajadoresRes.data);
      setFestivosGlobales(festivosRes.data);
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

  const handleAusenciaSave = () => {
    setIsAusenciaFormOpen(false);
    // Si estábamos viendo las ausencias de un trabajador, las recargamos
    if (selectedTrabajadorAusencias) {
      handleViewAusencias(selectedTrabajadorAusencias);
    }
    // También recargamos los festivos globales
    apiClient.get('/admin/bloqueos/globales').then(res => setFestivosGlobales(res.data));
  };
  const handleFestivoSave = () => {
    setIsFestivoFormOpen(false);
    fetchData(); // Recargamos todos los datos
  };

  const handleViewAusencias = async (trabajador) => {
    setSelectedTrabajadorAusencias(trabajador);
    try {
      const response = await apiClient.get(`/admin/bloqueos/trabajador/${trabajador.id}`);
      setAusencias(response.data);
    } catch (error) {
      toast.error("No se pudieron cargar las ausencias.");
    }
  };

  const handleCloseAusencias = () => {
    setSelectedTrabajadorAusencias(null);
    setAusencias([]);
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

  const handleDeleteAusencia = async (id) => {
    try {
      await apiClient.delete(`/admin/bloqueos/${id}`);
      toast.success('El periodo de ausencia ha sido eliminado.');
      fetchData(); // Recargamos todo para mantener la consistencia
      if (selectedTrabajadorAusencias) {
        handleViewAusencias(selectedTrabajadorAusencias);
      }
    } catch (error) {
      toast.error("No se pudo eliminar el periodo de ausencia.");
    }
  };

  if (loading) return <div className="flex justify-center items-center h-64"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>;
  if (error) return <div className="text-center text-red-500 bg-red-100 p-4 rounded-md">{error}</div>;

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Gestión de Trabajadores</h1>
        <div className="flex gap-2">
          <Button onClick={() => setIsFestivoFormOpen(true)} variant="secondary" className="flex items-center gap-2"><CalendarOff size={20} />Añadir Festivo</Button>
          <Button onClick={() => setIsAusenciaFormOpen(true)} variant="outline" className="flex items-center gap-2"><CalendarOff size={20} />Añadir Ausencia</Button>
          <Button onClick={() => handleOpenForm()} className="bg-cyan-600 hover:bg-cyan-700 text-white flex items-center gap-2"><PlusCircle size={20} />Añadir Trabajador</Button>
        </div>
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg shadow-md overflow-hidden">
          <Table>
            <TableHeader className="bg-gray-50">
              <TableRow>
                <TableHead className="font-bold">Nombre</TableHead>
                <TableHead className="font-bold">Estado</TableHead>
                <TableHead className="font-bold text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredTrabajadores.length > 0 ? (
                filteredTrabajadores.map((trabajador) => (
                  <TableRow
                    key={trabajador.id}
                    onClick={() => handleViewAusencias(trabajador)}
                    className={`cursor-pointer hover:bg-cyan-50 ${selectedTrabajadorAusencias?.id === trabajador.id ? 'bg-cyan-100' : ''} ${!trabajador.activo ? 'bg-gray-100 text-gray-500 hover:bg-gray-200' : ''}`}
                  >
                    <TableCell>{trabajador.nombre} {trabajador.apellidos}</TableCell>
                    <TableCell>
                      {trabajador.activo ? (
                        <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">Activo</span>
                      ) : (
                        <span className="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Inactivo</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button onClick={(e) => { e.stopPropagation(); handleOpenForm(trabajador); }} variant="ghost" size="icon" className="text-blue-600 hover:text-blue-800"><Edit size={18} /></Button>
                      <Button onClick={(e) => { e.stopPropagation(); setDeletingTrabajadorId(trabajador.id); }} variant="ghost" size="icon" className="text-red-600 hover:text-red-800"><Trash2 size={18} /></Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow><TableCell colSpan="3" className="text-center">No se encontraron trabajadores.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </div>

        {/* Vista de Ausencias */}
        <div className="lg:sticky lg:top-24 h-fit lg:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Tarjeta de Festivos Globales */}
          <Card>
            <CardHeader><CardTitle>Festivos Globales</CardTitle></CardHeader>
            <CardContent>
              {festivosGlobales.length > 0 ? (
                <ul className="space-y-3 max-h-60 overflow-y-auto pr-2">
                  {festivosGlobales.map(festivo => (
                    <li key={festivo.id} className="relative p-3 bg-gray-50 rounded-md border group">
                      <p className="font-semibold">{festivo.motivo}</p>
                      <p className="text-sm text-gray-600">
                        {new Date(festivo.fechaHoraInicio).toLocaleDateString('es-ES')} - {new Date(festivo.fechaHoraFin).toLocaleDateString('es-ES')}
                      </p>
                      <Button onClick={() => handleDeleteAusencia(festivo.id)} variant="ghost" size="icon" className="absolute top-1 right-1 h-7 w-7 text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"><Trash2 size={16} /></Button>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-sm text-gray-500">No hay festivos globales registrados.</p>}
            </CardContent>
          </Card>

          {/* Tarjeta de Ausencias del Trabajador */}
          {selectedTrabajadorAusencias ? (
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>Ausencias de {selectedTrabajadorAusencias.nombre}</CardTitle>
                <Button variant="ghost" size="icon" onClick={handleCloseAusencias}><X className="h-4 w-4" /></Button>
              </CardHeader>
              <CardContent className="max-h-60 overflow-y-auto pr-2">
                {ausencias.length > 0 ? (
                  <ul className="space-y-3">
                    {ausencias.map(ausencia => (
                      <li key={ausencia.id} className="relative p-3 bg-gray-50 rounded-md border group">
                        <p className="font-semibold">{ausencia.motivo}</p>
                        <p className="text-sm text-gray-600">
                          Desde: {new Date(ausencia.fechaHoraInicio).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' })}
                        </p>
                        <p className="text-sm text-gray-600">
                          Hasta: {new Date(ausencia.fechaHoraFin).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' })}
                        </p>
                        <Button onClick={() => handleDeleteAusencia(ausencia.id)} variant="ghost" size="icon" className="absolute top-1 right-1 h-7 w-7 text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"><Trash2 size={16} /></Button>
                      </li>
                    ))}
                  </ul>
                ) : <p className="text-sm text-gray-500">Este trabajador no tiene ausencias registradas.</p>}
              </CardContent>
            </Card>
          ) : (
            <div className="flex items-center justify-center h-full bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 p-8">
              <p className="text-center text-gray-500">Selecciona un trabajador para ver sus ausencias.</p>
            </div>
          )}
        </div>
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-[425px] bg-white">
          <DialogHeader>
            <DialogTitle>{editingTrabajador ? 'Editar Trabajador' : 'Añadir Trabajador'}</DialogTitle>
          </DialogHeader>
          <TrabajadorForm trabajador={editingTrabajador} onSave={handleSave} onCancel={() => setIsFormOpen(false)} />
        </DialogContent>
      </Dialog>

      <Dialog open={isAusenciaFormOpen} onOpenChange={setIsAusenciaFormOpen}>
        <DialogContent className="sm:max-w-md bg-white">
          <DialogHeader>
            <DialogTitle>Registrar Nueva Ausencia</DialogTitle>
          </DialogHeader>
          <FormularioAusencia onSave={handleAusenciaSave} onCancel={() => setIsAusenciaFormOpen(false)} />
        </DialogContent>
      </Dialog>

      <Dialog open={isFestivoFormOpen} onOpenChange={setIsFestivoFormOpen}>
        <DialogContent className="sm:max-w-md bg-white">
          <DialogHeader>
            <DialogTitle>Registrar Nuevo Festivo</DialogTitle>
          </DialogHeader>
          <FormularioAusencia tipoBloqueo="GLOBAL" onSave={handleFestivoSave} onCancel={() => setIsFestivoFormOpen(false)} />
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
