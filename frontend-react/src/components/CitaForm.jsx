import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { apiClient } from '../router/api.js';
import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { CalendarIcon, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

const formSchema = z.object({
  idCliente: z.string().min(1, "El cliente es obligatorio."),
  idServicio: z.string().min(1, "El servicio es obligatorio."),
  idTrabajador: z.string().min(1, "El trabajador es obligatorio."),
  fecha: z.date({ required_error: "La fecha es obligatoria." }),
  hora: z.string().min(1, "La hora es obligatoria."),
});

const CitaForm = ({ cita, onSave, onCancel }) => {
  const [clientes, setClientes] = useState([]);
  const [servicios, setServicios] = useState([]);
  const [trabajadores, setTrabajadores] = useState([]);
  const [horarios, setHorarios] = useState([]);
  const [loadingHorarios, setLoadingHorarios] = useState(false);

  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      idCliente: cita?.cliente.id.toString() || '',
      idServicio: cita?.servicio.id.toString() || '',
      idTrabajador: cita?.trabajador.id.toString() || '',
      fecha: cita ? new Date(cita.fechaHoraInicio) : new Date(),
      hora: cita ? format(new Date(cita.fechaHoraInicio), 'HH:mm') : '',
    },
  });

  const idServicio = form.watch('idServicio');
  const idTrabajador = form.watch('idTrabajador');
  const fecha = form.watch('fecha');

  useEffect(() => {
    const fetchData = async () => {
      const [clientesRes, serviciosRes, trabajadoresRes] = await Promise.all([
        apiClient.get('/admin/clientes'),
        apiClient.get('/servicios'),
        apiClient.get('/admin/trabajadores'),
      ]);
      setClientes(clientesRes.data);
      setServicios(serviciosRes.data);
      setTrabajadores(trabajadoresRes.data);
    };
    fetchData();
  }, []);

  useEffect(() => {
    if (idServicio && idTrabajador && fecha) {
      const fetchHorarios = async () => {
        setLoadingHorarios(true);
        try {
          const formattedDate = format(fecha, 'yyyy-MM-dd');
          const res = await apiClient.get(`/citas/disponibilidad?servicioId=${idServicio}&trabajadorId=${idTrabajador}&fecha=${formattedDate}`);
          setHorarios(res.data);
        } catch (error) {
          console.error("Error fetching horarios", error);
          setHorarios([]);
        } finally {
          setLoadingHorarios(false);
        }
      };
      fetchHorarios();
    }
  }, [idServicio, idTrabajador, fecha]);

  const onSubmit = async (values) => {
    try {
      const [hora, minuto] = values.hora.split(':');
      const fechaHoraInicio = new Date(values.fecha);
      fechaHoraInicio.setHours(parseInt(hora), parseInt(minuto));

      const payload = {
        idCliente: parseInt(values.idCliente),
        idServicio: parseInt(values.idServicio),
        idTrabajador: parseInt(values.idTrabajador),
        fechaHoraInicio: fechaHoraInicio.toISOString(),
      };

      if (cita) {
        await apiClient.put(`/admin/citas/${cita.id}`, payload);
        toast.success('Cita actualizada con éxito');
      } else {
        await apiClient.post('/admin/citas', payload);
        toast.success('Cita creada con éxito');
      }
      onSave();
    } catch (error) {
      console.error('Error guardando la cita:', error);
      toast.error(error.response?.data?.message || 'No se pudo guardar la cita.');
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField name="idCliente" control={form.control} render={({ field }) => (
          <FormItem>
            <FormLabel>Cliente</FormLabel>
            <Select onValueChange={field.onChange} defaultValue={field.value}>
              <FormControl><SelectTrigger><SelectValue placeholder="Selecciona un cliente" /></SelectTrigger></FormControl>
              <SelectContent className="bg-white">{clientes.map(c => <SelectItem key={c.id} value={c.id.toString()}>{c.nombre} {c.apellidos}</SelectItem>)}</SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        )} />
        <FormField name="idServicio" control={form.control} render={({ field }) => (
          <FormItem>
            <FormLabel>Servicio</FormLabel>
            <Select onValueChange={field.onChange} defaultValue={field.value}>
              <FormControl><SelectTrigger><SelectValue placeholder="Selecciona un servicio" /></SelectTrigger></FormControl>
              <SelectContent className="bg-white">{servicios.map(s => <SelectItem key={s.id} value={s.id.toString()}>{s.nombre}</SelectItem>)}</SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        )} />
        <FormField name="idTrabajador" control={form.control} render={({ field }) => (
          <FormItem>
            <FormLabel>Trabajador</FormLabel>
            <Select onValueChange={field.onChange} defaultValue={field.value}>
              <FormControl><SelectTrigger><SelectValue placeholder="Selecciona un trabajador" /></SelectTrigger></FormControl>
              <SelectContent className="bg-white">{trabajadores.map(t => <SelectItem key={t.id} value={t.id.toString()}>{t.nombre} {t.apellidos}</SelectItem>)}</SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        )} />
        <FormField name="fecha" control={form.control} render={({ field }) => (
          <FormItem className="flex flex-col">
            <FormLabel>Fecha</FormLabel>
            <Popover>
              <PopoverTrigger asChild>
                <FormControl>
                  <Button variant="outline" className={`w-full justify-start text-left font-normal ${!field.value && "text-muted-foreground"}`}>
                    <CalendarIcon className="mr-2 h-4 w-4" />
                    {field.value ? format(field.value, 'PPP', { locale: es }) : <span>Selecciona una fecha</span>}
                  </Button>
                </FormControl>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0 bg-white"><Calendar mode="single" selected={field.value} onSelect={field.onChange} initialFocus /></PopoverContent>
            </Popover>
            <FormMessage />
          </FormItem>
        )} />
        <FormField name="hora" control={form.control} render={({ field }) => (
          <FormItem>
            <FormLabel>Hora</FormLabel>
            <Select onValueChange={field.onChange} defaultValue={field.value} disabled={loadingHorarios || horarios.length === 0}>
              <FormControl><SelectTrigger><SelectValue placeholder={loadingHorarios ? "Cargando..." : "Selecciona una hora"} /></SelectTrigger></FormControl>
              <SelectContent className="bg-white">{horarios.map(h => <SelectItem key={h} value={h}>{h}</SelectItem>)}</SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        )} />
        <div className="flex justify-end gap-2 pt-4">
          <Button type="button" variant="ghost" onClick={onCancel}>Cancelar</Button>
          <Button type="submit" className="bg-cyan-600 hover:bg-cyan-700" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Guardar
          </Button>
        </div>
      </form>
    </Form>
  );
};

export default CitaForm;
