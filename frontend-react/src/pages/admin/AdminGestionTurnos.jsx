import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PlusCircle, Edit, Trash2 } from 'lucide-react';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";

const diasSemana = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"];

export default function GestionarTurnos() {
  const [trabajadores, setTrabajadores] = useState([]);
  const [trabajadorSeleccionado, setTrabajadorSeleccionado] = useState(null);
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [turnoActual, setTurnoActual] = useState(null); // Para editar

  // Cargar la lista de trabajadores al montar el componente
  useEffect(() => {
    apiClient.get('/admin/trabajadores')
      .then(response => {
        setTrabajadores(response.data);
        if (response.data.length > 0) {
          setTrabajadorSeleccionado(response.data[0].id.toString());
        }
      })
      .catch(error => console.error("Error al cargar trabajadores:", error));
  }, []);

  // Cargar los turnos cuando se selecciona un trabajador
  useEffect(() => {
    if (trabajadorSeleccionado) {
      setLoading(true);
      apiClient.get(`/admin/trabajadores/${trabajadorSeleccionado}/turnos`)
        .then(response => {
          setTurnos(response.data);
        })
        .catch(error => console.error("Error al cargar turnos:", error))
        .finally(() => setLoading(false));
    }
  }, [trabajadorSeleccionado]);

  const handleGuardarTurno = (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
      trabajadorId: trabajadorSeleccionado,
      diaDeLaSemana: formData.get('diaDeLaSemana'),
      horaInicio: formData.get('horaInicio'),
      horaFin: formData.get('horaFin'),
    };

    const promise = turnoActual
      ? apiClient.put(`/admin/turnos/${turnoActual.id}`, data)
      : apiClient.post('/admin/turnos', data);

    promise.then(() => {
      // Recargar turnos
      apiClient.get(`/admin/trabajadores/${trabajadorSeleccionado}/turnos`).then(res => setTurnos(res.data));
      setIsDialogOpen(false);
      setTurnoActual(null);
    }).catch(error => console.error("Error al guardar el turno:", error));
  };

  const handleEliminarTurno = (id) => {
    apiClient.delete(`/admin/turnos/${id}`)
      .then(() => {
        setTurnos(turnos.filter(t => t.id !== id));
      })
      .catch(error => console.error("Error al eliminar el turno:", error));
  };

  const abrirDialogoParaCrear = () => {
    setTurnoActual(null);
    setIsDialogOpen(true);
  };

  const abrirDialogoParaEditar = (turno) => {
    setTurnoActual(turno);
    setIsDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Gestionar Turnos</h1>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Turnos por Trabajador</CardTitle>
          <Button onClick={abrirDialogoParaCrear} disabled={!trabajadorSeleccionado}>
            <PlusCircle className="mr-2 h-4 w-4" /> Añadir Turno
          </Button>
        </CardHeader>
        <CardContent>
          <div className="mb-4">
            <Label htmlFor="trabajador-select">Selecciona un trabajador</Label>
            <Select onValueChange={setTrabajadorSeleccionado} value={trabajadorSeleccionado}>
              <SelectTrigger id="trabajador-select">
                <SelectValue placeholder="Selecciona un trabajador..." />
              </SelectTrigger>
              <SelectContent className="bg-white">
                {trabajadores.map(t => (
                  <SelectItem key={t.id} value={t.id.toString()}>
                    {t.nombre} {t.apellidos}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {loading ? (
            <p>Cargando turnos...</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Día de la Semana</TableHead>
                  <TableHead>Hora de Inicio</TableHead>
                  <TableHead>Hora de Fin</TableHead>
                  <TableHead className="text-right">Acciones</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {turnos.length > 0 ? turnos.map(turno => (
                  <TableRow key={turno.id}>
                    <TableCell>{turno.diaDeLaSemana}</TableCell>
                    <TableCell>{turno.horaInicio}</TableCell>
                    <TableCell>{turno.horaFin}</TableCell>
                    <TableCell className="text-right space-x-2">
                      <Button variant="outline" size="icon" onClick={() => abrirDialogoParaEditar(turno)}>
                        <Edit className="h-4 w-4" />
                      </Button>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button variant="destructive" size="icon">
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>¿Estás seguro?</AlertDialogTitle>
                            <AlertDialogDescription>
                              Esta acción no se puede deshacer. Se eliminará permanentemente el turno.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancelar</AlertDialogCancel>
                            <AlertDialogAction onClick={() => handleEliminarTurno(turno.id)}>
                              Eliminar
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </TableCell>
                  </TableRow>
                )) : (
                  <TableRow>
                    <TableCell colSpan="4" className="text-center">Este trabajador no tiene turnos asignados.</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Dialogo para Crear/Editar Turno */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="bg-white">
          <DialogHeader>
            <DialogTitle>{turnoActual ? 'Editar Turno' : 'Añadir Nuevo Turno'}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleGuardarTurno} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="diaDeLaSemana">Día de la Semana</Label>
              <Select name="diaDeLaSemana" defaultValue={turnoActual?.diaDeLaSemana}>
                <SelectTrigger>
                  <SelectValue placeholder="Selecciona un día" />
                </SelectTrigger>
                <SelectContent className="bg-white">
                  {diasSemana.map(dia => (
                    <SelectItem key={dia} value={dia}>{dia}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="horaInicio">Hora de Inicio</Label>
                <Input id="horaInicio" name="horaInicio" type="time" defaultValue={turnoActual?.horaInicio} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="horaFin">Hora de Fin</Label>
                <Input id="horaFin" name="horaFin" type="time" defaultValue={turnoActual?.horaFin} required />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="ghost" onClick={() => setIsDialogOpen(false)}>Cancelar</Button>
              <Button type="submit" className="bg-cyan-600 hover:bg-cyan-700 text-white">Guardar</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}