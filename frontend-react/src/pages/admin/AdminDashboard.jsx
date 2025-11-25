import { useState, useEffect } from 'react';
import { apiClient } from '../../router/api';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2, Users, Calendar, Scissors, UserCheck } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        // Hacemos todas las llamadas a la API en paralelo para mayor eficiencia
        const [clientesRes, trabajadoresRes, citasRes, chartDataRes] = await Promise.all([
          apiClient.get('/admin/clientes'),
          apiClient.get('/admin/trabajadores'),
          apiClient.get('/admin/citas'),
          apiClient.get('/admin/stats/citas-semanales') // Nuevo endpoint para el gráfico
        ]);

        const hoy = new Date().toISOString().split('T')[0];
        const citasHoy = citasRes.data.filter(c => c && c.fecha && typeof c.fecha === 'string' && c.fecha.startsWith(hoy));

        setStats({
          totalClientes: clientesRes.data.length,
          totalTrabajadores: trabajadoresRes.data.filter(t => t.activo).length,
          citasHoy: citasHoy.length,
          proximasCitas: citasHoy.slice(0, 5), // Las primeras 5 de hoy
          chartData: chartDataRes.data, // Usamos los datos reales del backend
        });

      } catch (error) {
        console.error("Error al cargar los datos del dashboard:", error);
        // Aquí podrías poner valores por defecto en caso de error
        setStats({ totalClientes: 0, totalTrabajadores: 0, citasHoy: 0, proximasCitas: [], chartData: [] });
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Loader2 className="h-8 w-8 animate-spin text-cyan-600" /></div>;
  }

  return (
    <div className="p-4 space-y-6">
      <h1 className="text-3xl font-bold text-gray-800">Dashboard de Administración</h1>

      {/* 1. Tarjetas de Estadísticas (KPIs) */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Clientes</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.totalClientes}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Trabajadores Activos</CardTitle>
            <UserCheck className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.totalTrabajadores}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Citas para Hoy</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.citasHoy}</div></CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-7">
        {/* 2. Gráfico de Actividad */}
        <Card className="lg:col-span-4">
          <CardHeader><CardTitle>Citas de la Última Semana</CardTitle></CardHeader>
          <CardContent className="pl-2">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={stats.chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="Citas" fill="#0891b2" name="Nº de Citas" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* 3. Próximas Citas */}
        <Card className="lg:col-span-3">
          <CardHeader><CardTitle>Próximas Citas de Hoy</CardTitle></CardHeader>
          <CardContent>
            <div className="space-y-4">
              {stats.proximasCitas.length > 0 ? stats.proximasCitas.map(cita => (
                <div key={cita.id} className="flex items-center">
                  <Calendar className="h-5 w-5 text-cyan-500 mr-4" />
                  <div className="flex-grow">
                    <p className="text-sm font-medium leading-none">{cita.usuario.nombre} {cita.usuario.apellidos}</p>
                    <p className="text-sm text-muted-foreground">{cita.servicio.nombre} con {cita.trabajador.nombre}</p>
                  </div>
                  <div className="ml-auto font-medium">{cita.hora}</div>
                </div>
              )) : <p className="text-sm text-muted-foreground">No hay más citas programadas para hoy.</p>}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default AdminDashboard;
