import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { apiClient } from '../router/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

const formSchema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio."),
  descripcion: z.string().optional(),
  precio: z.coerce.number().min(0, "El precio no puede ser negativo."),
  duracionEnMinutos: z.coerce.number().int().min(1, "La duración debe ser de al menos 1 minuto."),
  tipoServicioId: z.string().min(1, "El tipo de servicio es obligatorio."),
});

const ServicioForm = ({ servicio, onSave, onCancel }) => {
  const [tiposServicio, setTiposServicio] = useState([]);

  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      nombre: servicio?.nombre || '',
      descripcion: servicio?.descripcion || '',
      precio: servicio?.precio || 0,
      duracionEnMinutos: servicio?.duracionEnMinutos || 30,
      tipoServicioId: servicio?.tipoServicio.id.toString() || '',
    },
  });

  useEffect(() => {
    const fetchTiposServicio = async () => {
      try {
        const response = await apiClient.get('/tiposervicios');
        setTiposServicio(response.data);
      } catch (error) {
        console.error("Error fetching tipos de servicio", error);
      }
    };
    fetchTiposServicio();
  }, []);

  const onSubmit = async (values) => {
    try {
      const payload = {
        ...values,
        tipoServicioId: parseInt(values.tipoServicioId),
      };

      if (servicio) {
        await apiClient.put(`/admin/servicios/${servicio.id}`, payload);
        toast.success('Servicio actualizado con éxito');
      } else {
        await apiClient.post('/admin/servicios', payload);
        toast.success('Servicio creado con éxito');
      }
      onSave();
    } catch (error) {
      console.error('Error guardando el servicio:', error);
      toast.error(error.response?.data?.message || 'No se pudo guardar el servicio.');
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField name="nombre" control={form.control} render={({ field }) => (
          <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
        )} />
        <FormField name="descripcion" control={form.control} render={({ field }) => (
          <FormItem><FormLabel>Descripción</FormLabel><FormControl><Textarea {...field} /></FormControl><FormMessage /></FormItem>
        )} />
        <div className="grid grid-cols-2 gap-4">
          <FormField name="precio" control={form.control} render={({ field }) => (
            <FormItem><FormLabel>Precio (€)</FormLabel><FormControl><Input type="number" step="0.01" {...field} /></FormControl><FormMessage /></FormItem>
          )} />
          <FormField name="duracionEnMinutos" control={form.control} render={({ field }) => (
            <FormItem><FormLabel>Duración (min)</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
          )} />
        </div>
        <FormField name="tipoServicioId" control={form.control} render={({ field }) => (
          <FormItem>
            <FormLabel>Tipo de Servicio</FormLabel>
            <Select onValueChange={field.onChange} defaultValue={field.value}>
              <FormControl><SelectTrigger><SelectValue placeholder="Selecciona un tipo" /></SelectTrigger></FormControl>
              <SelectContent className="bg-white">{tiposServicio.map(t => <SelectItem key={t.id} value={t.id.toString()}>{t.nombre}</SelectItem>)}</SelectContent>
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

export default ServicioForm;
