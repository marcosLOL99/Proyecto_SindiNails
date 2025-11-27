import { useState, useEffect } from 'react';
import { apiClient } from '../router/api';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

const baseSchema = {
  fechaInicio: z.string().min(1, "La fecha de inicio es obligatoria."),
  horaInicio: z.string().min(1, "La hora de inicio es obligatoria."),
  fechaFin: z.string().min(1, "La fecha de fin es obligatoria."),
  horaFin: z.string().min(1, "La hora de fin es obligatoria."),
  motivo: z.string().min(3, "El motivo debe tener al menos 3 caracteres."),
};

const FormularioAusencia = ({ onSave, onCancel, tipoBloqueo = 'TRABAJADOR' }) => {
  const [trabajadores, setTrabajadores] = useState([]);
  const [loading, setLoading] = useState(false);

  const formSchema = z.object({
    ...baseSchema,
    ...(tipoBloqueo === 'TRABAJADOR' && { trabajadorId: z.string().min(1, "Debes seleccionar un trabajador.") }),
  }).refine(data => `${data.fechaFin}T${data.horaFin}` > `${data.fechaInicio}T${data.horaInicio}`, {
  message: "La fecha y hora de fin debe ser posterior a la de inicio.",
  path: ["fechaFin"],
});

  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      trabajadorId: '',
      fechaInicio: new Date().toISOString().split('T')[0],
      horaInicio: '09:00',
      fechaFin: '',
      horaFin: '20:00',
      motivo: '',
    },
  });

  useEffect(() => {
    if (tipoBloqueo === 'TRABAJADOR') {
      const fetchTrabajadores = async () => {
        try {
          const response = await apiClient.get('/admin/trabajadores');
          setTrabajadores(response.data.filter(t => t.activo)); // Solo trabajadores activos
        } catch (error) {
          toast.error("No se pudieron cargar los trabajadores.");
        }
      };
      fetchTrabajadores();
    }
  }, [tipoBloqueo]);

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      const payload = {
        trabajadorId: values.trabajadorId ? parseInt(values.trabajadorId, 10) : null,
        fechaHoraInicio: `${values.fechaInicio}T${values.horaInicio}:00`,
        fechaHoraFin: `${values.fechaFin}T${values.horaFin}:00`,
        motivo: values.motivo,
        tipo: tipoBloqueo,
      };
      await apiClient.post('/admin/bloqueos', payload);
      toast.success('Ausencia registrada con éxito.');
      onSave();
    } catch (error) {
      console.error("Error al crear ausencia:", error);
      toast.error(error.response?.data?.message || 'No se pudo registrar la ausencia.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {tipoBloqueo === 'TRABAJADOR' && (
          <FormField
            control={form.control}
            name="trabajadorId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Trabajador</FormLabel>
                <Select onValueChange={field.onChange} defaultValue={field.value}>
                  <FormControl>
                    <SelectTrigger><SelectValue placeholder="Selecciona un trabajador" /></SelectTrigger>
                  </FormControl>
                  <SelectContent className="bg-white">
                    {trabajadores.map(t => <SelectItem key={t.id} value={String(t.id)}>{t.nombre} {t.apellidos}</SelectItem>)}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        <div className="grid grid-cols-2 gap-4">
          <FormField control={form.control} name="fechaInicio" render={({ field }) => (<FormItem><FormLabel>Fecha Inicio</FormLabel><FormControl><Input type="date" {...field} /></FormControl><FormMessage /></FormItem>)} />
          <FormField control={form.control} name="horaInicio" render={({ field }) => (<FormItem><FormLabel>Hora Inicio</FormLabel><FormControl><Input type="time" {...field} /></FormControl><FormMessage /></FormItem>)} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <FormField control={form.control} name="fechaFin" render={({ field }) => (<FormItem><FormLabel>Fecha Fin</FormLabel><FormControl><Input type="date" {...field} /></FormControl><FormMessage /></FormItem>)} />
          <FormField control={form.control} name="horaFin" render={({ field }) => (<FormItem><FormLabel>Hora Fin</FormLabel><FormControl><Input type="time" {...field} /></FormControl><FormMessage /></FormItem>)} />
        </div>

        <FormField
          control={form.control}
          name="motivo"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{tipoBloqueo === 'TRABAJADOR' ? 'Motivo de la ausencia' : 'Motivo del festivo'}</FormLabel>
              <FormControl><Input placeholder="Ej: Vacaciones, Cita médica..." {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="flex justify-end gap-4 pt-4">
          <Button type="button" variant="ghost" onClick={onCancel}>Cancelar</Button>
          <Button type="submit" disabled={loading} className="bg-cyan-600 hover:bg-cyan-700">
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Guardar
          </Button>
        </div>
      </form>
    </Form>
  );
};

export default FormularioAusencia;