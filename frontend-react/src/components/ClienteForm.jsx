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
  email: z.string().email("Email no válido."),
  telefono: z.string().min(9, "El teléfono debe tener al menos 9 dígitos.").max(15, "El teléfono no puede tener más de 15 dígitos."),
  activo: z.boolean().default(true),
});

const ClienteForm = ({ cliente, onSave, onCancel }) => {
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      nombre: '',
      apellidos: '',
      email: '',
      telefono: '',
      activo: true,
    },
  });

  useEffect(() => {
    if (cliente) {
      form.reset({
        nombre: cliente.nombre || '',
        apellidos: cliente.apellidos || '',
        email: cliente.email || '',
        telefono: cliente.telefono || '',
        activo: cliente.activo ?? true,
      });
    } else {
      form.reset({ nombre: '', apellidos: '', email: '', telefono: '', activo: true });
    }
  }, [cliente, form]);

  const onSubmit = async (values) => {
    try {
      if (cliente) {
        await apiClient.put(`/admin/clientes/${cliente.id}`, values);
        toast.success('Cliente actualizado con éxito');
      } else {
        await apiClient.post('/admin/clientes', values);
        toast.success('Cliente creado con éxito');
      }
      onSave();
    } catch (error) {
      console.error('Error guardando el cliente:', error);
      toast.error(error.response?.data?.message || 'No se pudo guardar el cliente.');
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField name="nombre" control={form.control} render={({ field }) => (<FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>)} />
        <FormField name="apellidos" control={form.control} render={({ field }) => (<FormItem><FormLabel>Apellidos</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>)} />
        <FormField name="email" control={form.control} render={({ field }) => (<FormItem><FormLabel>Email</FormLabel><FormControl><Input type="email" {...field} /></FormControl><FormMessage /></FormItem>)} />
        <FormField name="telefono" control={form.control} render={({ field }) => (<FormItem><FormLabel>Teléfono</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>)} />
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

export default ClienteForm;
