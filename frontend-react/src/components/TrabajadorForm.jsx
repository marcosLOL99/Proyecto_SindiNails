import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { apiClient } from '../router/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

const formSchema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio."),
  apellidos: z.string().min(1, "Los apellidos son obligatorios."),
  activo: z.boolean().default(true),
});

const TrabajadorForm = ({ trabajador, onSave, onCancel }) => {
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      nombre: '',
      apellidos: '',
      activo: true,
    },
  });

  // useEffect para resetear el formulario si el trabajador cambia (para edición)
  useEffect(() => {
    if (trabajador) {
      form.reset({
        nombre: trabajador.nombre || '',
        apellidos: trabajador.apellidos || '',
        activo: trabajador.activo ?? true,
      });
    } else {
      form.reset({
        nombre: '',
        apellidos: '',
        activo: true,
      });
    }
  }, [trabajador, form]);

  const onSubmit = async (values) => {
    try {
      if (trabajador) {
        // El payload se envía completo desde 'values'
        await apiClient.put(`/admin/trabajadores/${trabajador.id}`, values); 
        toast.success('Trabajador actualizado con éxito');
      } else {
        await apiClient.post('/admin/trabajadores', values);
        toast.success('Trabajador creado con éxito');
      }
      onSave();
    } catch (error) {
      console.error('Error guardando el trabajador:', error);
      toast.error(error.response?.data?.message || 'No se pudo guardar el trabajador.');
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField name="nombre" control={form.control} render={({ field }) => (
          <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
        )} />
        <FormField name="apellidos" control={form.control} render={({ field }) => (
          <FormItem><FormLabel>Apellidos</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
        )} />
        <FormField
          control={form.control}
          name="activo"
          render={({ field }) => (
            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3 shadow-sm">
              <div className="space-y-0.5">
                <FormLabel>Activo</FormLabel>
                <FormMessage />
              </div>
              <FormControl>
                <Switch
                  checked={field.value}
                  onCheckedChange={field.onChange}
                  className="data-[state=checked]:bg-green-500 data-[state=unchecked]:bg-red-500"
                /></FormControl>
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

export default TrabajadorForm;
