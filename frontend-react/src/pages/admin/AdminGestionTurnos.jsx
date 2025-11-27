import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import { format, startOfWeek, endOfWeek, eachDayOfInterval, addDays } from 'date-fns';
import { es } from 'date-fns/locale';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { PlusCircle, Edit, Trash2, Calendar as CalendarIcon, ChevronLeft, ChevronRight } from 'lucide-react';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";

const initialHorarioState = Array(7).fill(null).map(() => ({
  activo: false,
  horaInicio: '', horaFin: '',
  horaInicioDescanso: '', horaFinDescanso: ''
}));

export default function AdminGestionTurnos() {
  const [trabajadores, setTrabajadores] = useState([]);
  const [trabajadorSeleccionado, setTrabajadorSeleccionado] = useState(null);
  const [turnosPorSemana, setTurnosPorSemana] = useState({});
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
          // Agrupar turnos por semana
          const grouped = response.data.reduce((acc, turno) => {
            const fecha = new Date(turno.fecha);
            const inicioSemana = startOfWeek(fecha, { weekStartsOn: 1 });
            const key = format(inicioSemana, 'yyyy-MM-dd');
            if (!acc[key]) {
              acc[key] = [];
            }
            acc[key].push(turno);
            return acc;
          }, {});
          setTurnosPorSemana(grouped);
        })
        .catch(error => console.error("Error al cargar turnos:", error))
        .finally(() => setLoading(false));
    }
  }, [trabajadorSeleccionado]);

  const handleGuardarTurno = (e) => {
    e.preventDefault();
    const payload = {
      trabajadorId: trabajadorSeleccionado,
      fechaInicioSemana: format(startOfWeek(semanaSeleccionada, { weekStartsOn: 1 }), 'yyyy-MM-dd'),
      horarios: horariosSemana.reduce((acc, horario, index) => {
        acc[index] = {
          ...horario,
          horaInicioDescanso: horario.horaInicioDescanso || null,
          horaFinDescanso: horario.horaFinDescanso || null,
        };
        return acc;
      }, {})
    };

    apiClient.post('/admin/turnos/batch', payload)
      .then(() => {
        setIsDialogOpen(false);
        // Recargar turnos
        if (trabajadorSeleccionado) {
          apiClient.get(`/admin/trabajadores/${trabajadorSeleccionado}/turnos`)
            .then(response => {
              const grouped = response.data.reduce((acc, turno) => {
                const fecha = new Date(turno.fecha);
                const inicioSemana = startOfWeek(fecha, { weekStartsOn: 1 });
                const key = format(inicioSemana, 'yyyy-MM-dd');
                if (!acc[key]) {
                  acc[key] = [];
                }
                acc[key].push(turno);
                return acc;
              }, {});
              setTurnosPorSemana(grouped);
            });
        }
      })
      .catch(error => console.error("Error al guardar los turnos:", error));
  };

  const handleEliminarTurno = (id) => {
    apiClient.delete(`/admin/turnos/${id}`)
      .then(() => {
        // Optimistic update: remove the turn from the state
        const newTurnosPorSemana = { ...turnosPorSemana };
        for (const weekKey in newTurnosPorSemana) {
          newTurnosPorSemana[weekKey] = newTurnosPorSemana[weekKey].filter(t => t.id !== id);
          if (newTurnosPorSemana[weekKey].length === 0) {
            delete newTurnosPorSemana[weekKey];
          }
        }
        setTurnosPorSemana(newTurnosPorSemana);
      })
      .catch(error => console.error("Error al eliminar el turno:", error));
  };

  const [semanaSeleccionada, setSemanaSeleccionada] = useState(new Date());
  const [horariosSemana, setHorariosSemana] = useState(initialHorarioState);

  const abrirDialogoSemana = (fecha) => {
    setSemanaSeleccionada(fecha);
    setHorariosSemana(initialHorarioState); // Resetear al abrir
    setIsDialogOpen(true);
  }

  const handleHorarioChange = (index, field, value) => {
    const newHorarios = [...horariosSemana];
    newHorarios[index] = { ...newHorarios[index], [field]: value };

    // Si se está editando el Lunes (índice 0) y no es el checkbox de "activo",
    // replicar el cambio en los demás días que estén activos.
    if (index === 0 && field !== 'activo') {
      for (let i = 1; i < newHorarios.length; i++) {
        // Solo replicar si el día está marcado como activo
        if (newHorarios[i].activo) {
          newHorarios[i] = { ...newHorarios[i], [field]: value };
        }
      }
    }

    setHorariosSemana(newHorarios);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Gestionar Turnos</h1>
      <Card>
        <CardHeader>
          <CardTitle>Turnos de {trabajadores.find(t => t.id == trabajadorSeleccionado)?.nombre || '...'}</CardTitle>
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

          <div className="flex items-center justify-between mb-4 p-2 border rounded-md">
            <p className="font-semibold">Gestionar Turnos por Semana</p>
            <Button onClick={() => abrirDialogoSemana(new Date())} disabled={!trabajadorSeleccionado}>
              <PlusCircle className="mr-2 h-4 w-4" /> Añadir/Editar Turnos de una Semana
            </Button>
          </div>

          {loading ? (
            <p>Cargando turnos...</p>
          ) : (
            <div className="space-y-4">
              {Object.keys(turnosPorSemana).sort().reverse().map(weekKey => (
                <div key={weekKey}>
                  <h3 className="font-semibold mb-2">
                    Semana del {format(new Date(weekKey), "d 'de' MMMM", { locale: es })}
                  </h3>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Fecha</TableHead>
                        <TableHead>Hora de Inicio</TableHead>
                        <TableHead>Hora de Fin</TableHead>
                        <TableHead className="text-right">Acciones</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {turnosPorSemana[weekKey].sort((a, b) => new Date(a.fecha) - new Date(b.fecha) || a.horaInicio.localeCompare(b.horaInicio)).map(turno => (
                        <TableRow key={turno.id}>
                          <TableCell>{format(new Date(turno.fecha), "EEEE, d 'de' MMMM", { locale: es })}</TableCell>
                          <TableCell>{turno.horaInicio}</TableCell>
                          <TableCell>{turno.horaFin}</TableCell>
                          <TableCell className="text-right">
                            <AlertDialog>
                              <AlertDialogTrigger asChild>
                                <Button variant="destructive" size="icon">
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </AlertDialogTrigger>
                              <AlertDialogContent className="bg-white">
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
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ))}
              {Object.keys(turnosPorSemana).length === 0 && (
                <p className="text-center text-gray-500 py-4">Este trabajador no tiene turnos asignados.</p>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Dialogo para Crear/Editar Turnos de la Semana */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-4xl bg-white">
          <DialogHeader>
            <DialogTitle>Gestionar Turnos para la Semana</DialogTitle>
            <div className="flex items-center gap-4 pt-2">
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="w-[280px] justify-start text-left font-normal">
                    <CalendarIcon className="mr-2 h-4 w-4" />
                    {format(semanaSeleccionada, "PPP", { locale: es })}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0 bg-white">
                  <Calendar mode="single" selected={semanaSeleccionada} onSelect={setSemanaSeleccionada} initialFocus weekStartsOn={1} />
                </PopoverContent>
              </Popover>
              <p className="text-sm text-muted-foreground">
                Semana del {format(startOfWeek(semanaSeleccionada, { weekStartsOn: 1 }), 'dd/MM/yy')} al {format(endOfWeek(semanaSeleccionada, { weekStartsOn: 1 }), 'dd/MM/yy')}
              </p>
            </div>
          </DialogHeader>
          <form onSubmit={handleGuardarTurno} className="space-y-4">
            <div className="space-y-3 max-h-[50vh] overflow-y-auto p-1">
              {eachDayOfInterval({
                start: startOfWeek(semanaSeleccionada, { weekStartsOn: 1 }),
                end: endOfWeek(semanaSeleccionada, { weekStartsOn: 1 })
              }).map((day, index) => (
                <div key={index} className="p-4 border rounded-lg space-y-3">
                  <div className="flex items-center justify-between">
                    <Label htmlFor={`activo-${index}`} className="text-base font-semibold capitalize">
                      {format(day, "EEEE", { locale: es })}
                    </Label>
                    <Checkbox
                      id={`activo-${index}`}
                      checked={horariosSemana[index].activo}
                      onCheckedChange={(checked) => handleHorarioChange(index, 'activo', checked)}
                    />
                  </div>
                  <div className={`grid grid-cols-2 md:grid-cols-4 gap-4 transition-opacity ${horariosSemana[index].activo ? 'opacity-100' : 'opacity-50 pointer-events-none'}`}>
                    <div className="space-y-1">
                      <Label htmlFor={`horaInicio-${index}`}>Inicio</Label>
                      <Input id={`horaInicio-${index}`} type="time" value={horariosSemana[index].horaInicio} onChange={(e) => handleHorarioChange(index, 'horaInicio', e.target.value)} />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor={`horaFin-${index}`}>Fin</Label>
                      <Input id={`horaFin-${index}`} type="time" value={horariosSemana[index].horaFin} onChange={(e) => handleHorarioChange(index, 'horaFin', e.target.value)} />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor={`horaInicioDescanso-${index}`}>Inicio Descanso</Label>
                      <Input id={`horaInicioDescanso-${index}`} type="time" value={horariosSemana[index].horaInicioDescanso} onChange={(e) => handleHorarioChange(index, 'horaInicioDescanso', e.target.value)} />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor={`horaFinDescanso-${index}`}>Fin Descanso</Label>
                      <Input id={`horaFinDescanso-${index}`} type="time" value={horariosSemana[index].horaFinDescanso} onChange={(e) => handleHorarioChange(index, 'horaFinDescanso', e.target.value)} />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <DialogFooter>
              <Button type="button" variant="ghost" onClick={() => setIsDialogOpen(false)}>Cancelar</Button>
              <Button type="submit">Guardar</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
