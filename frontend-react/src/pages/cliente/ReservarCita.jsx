import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../../router/api.js';
import { useAuth } from '../../context/AuthContext';
import { motion } from 'framer-motion';
import { format, parse, addMinutes } from 'date-fns';
import { es } from 'date-fns/locale';

// Componentes de UI
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Loader2, User, Calendar as CalendarIcon, Clock, ArrowLeft, CheckCircle, AlertCircle } from 'lucide-react';

export default function ReservarCita() {
  const { servicioId } = useParams();
  const navigate = useNavigate();

  // Estados de datos
  const [servicio, setServicio] = useState(null);
  const [trabajadores, setTrabajadores] = useState([]);
  const [horasDisponibles, setHorasDisponibles] = useState([]);
  const [diasNoDisponibles, setDiasNoDisponibles] = useState([]);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  
  // Estados de selección del usuario
  const [selectedTrabajador, setSelectedTrabajador] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedTime, setSelectedTime] = useState(null);

  // Estados de UI
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [loadingHoras, setLoadingHoras] = useState(false);
  const [loadingDias, setLoadingDias] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        setLoading(true);
        const [servicioRes, trabajadoresRes] = await Promise.all([
          apiClient.get(`/servicios/${servicioId}`),
          apiClient.get('/trabajadores')
        ]);

        setServicio(servicioRes.data);
        setTrabajadores(trabajadoresRes.data);
        setError(null);
      } catch (err) {
        console.error("Error al cargar datos para la reserva:", err);
        setError("No se pudo cargar la información para la reserva. Por favor, vuelve a intentarlo.");
      } finally {
        setLoading(false);
      }
    };

    fetchInitialData();
  }, [servicioId]);

  // Efecto para cargar los días no disponibles cuando cambia el trabajador o el mes
  useEffect(() => {
    if (selectedTrabajador) {
      const fetchDiasNoDisponibles = async () => {
        setLoadingDias(true);
        try {
          const params = new URLSearchParams({
            trabajadorId: selectedTrabajador.id,
            servicioId: servicioId,
            anio: currentMonth.getFullYear(),
            mes: currentMonth.getMonth() + 1, // getMonth() es 0-indexado
          });
          const res = await apiClient.get(`/citas/dias-no-disponibles?${params.toString()}`);
          // Convertimos las fechas string del backend a objetos Date
          // Normalizamos las fechas para evitar problemas con la zona horaria.
          const disabledDates = res.data.map(dateStr => {
            const [year, month, day] = dateStr.split('-').map(Number);
            return new Date(year, month - 1, day);
          });
          setDiasNoDisponibles(disabledDates);
        } catch (err) {
          console.error("Error al cargar días no disponibles:", err);
        } finally {
          setLoadingDias(false);
        }
      };
      fetchDiasNoDisponibles();
    }
  }, [selectedTrabajador, servicioId, currentMonth]);

  // Efecto para cargar las horas disponibles cuando se selecciona trabajador y fecha
  useEffect(() => {
    if (selectedTrabajador && selectedDate) {
      const fetchHorasDisponibles = async () => {
        setLoadingHoras(true);
        setHorasDisponibles([]);
        setSelectedTime(null);
        try {
          const fechaFormateada = format(selectedDate, 'yyyy-MM-dd');
          const params = new URLSearchParams({
            trabajadorId: selectedTrabajador.id,
            servicioId: servicioId,
            fecha: fechaFormateada,
          });
          const res = await apiClient.get(`/citas/disponibilidad?${params.toString()}`);
          setHorasDisponibles(res.data);
        } catch (err) {
          console.error("Error al cargar horas disponibles:", err);
          setError("No se pudieron cargar los horarios. Inténtalo de nuevo.");
        } finally {
          setLoadingHoras(false);
        }
      };
      fetchHorasDisponibles();
    }
  }, [selectedTrabajador, selectedDate, servicioId]);

  const handleSelectTrabajador = (trabajador) => {
    setSelectedTrabajador(trabajador);
    setSelectedDate(null); // Reseteamos la fecha al cambiar de trabajador
    setSelectedTime(null); // Reseteamos la hora
    setHorasDisponibles([]);
  };

  const handleSelectTime = (hora) => {
    setSelectedTime(hora);
  };

  const handleConfirmBooking = async () => {
    if (!selectedTrabajador || !selectedDate || !selectedTime) {
      alert("Por favor, completa todos los pasos para reservar.");
      return;
    }
    setIsSubmitting(true);
    try {
      const [horas, minutos] = selectedTime.split(':');
      const fechaHoraInicio = new Date(selectedDate);
      fechaHoraInicio.setHours(parseInt(horas, 10));
      fechaHoraInicio.setMinutes(parseInt(minutos, 10));

      const payload = {
        idServicio: servicio.id,
        idTrabajador: selectedTrabajador.id,
        fechaHoraInicio: `${format(selectedDate, 'yyyy-MM-dd')}T${selectedTime}:00`,
      };

      await apiClient.post('/citas', payload);

      alert("¡Cita reservada con éxito!");
      navigate('/dashboard'); // Redirigir al panel del cliente
    } catch (err) {
      console.error("Error al confirmar la reserva:", err);
      alert(err.response?.data?.message || "No se pudo confirmar la reserva. El horario podría no estar ya disponible.");
    } finally {
      setIsSubmitting(false);
    }
  };
  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen bg-gray-50">
        <Loader2 className="h-12 w-12 animate-spin text-cyan-600" />
        <p className="ml-4 text-lg text-gray-600">Preparando tu reserva...</p>
      </div>
    );
  }

  if (error) {
    return <p className="text-center text-red-500 py-20">{error}</p>;
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      className="container mx-auto py-12 px-4 bg-gray-50"
    >
      <Button variant="outline" onClick={() => navigate('/servicios')} className="mb-8">
        <ArrowLeft className="mr-2 h-4 w-4" /> Volver a Servicios
      </Button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Columna de Resumen */}
        <div className="lg:col-span-1">
          <Card className="shadow-lg sticky top-8">
            <CardHeader className="pb-4">
              <CardTitle>Resumen de tu Cita</CardTitle>
              <CardDescription>Confirma los detalles de tu reserva.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 pt-2">
              <div className="font-bold text-lg text-cyan-600">{servicio?.nombre}</div>
              <div className="border-t border-gray-200 pt-4 space-y-3 text-gray-700">
                {selectedTrabajador && <div className="flex items-center gap-2"><User size={16} /> {selectedTrabajador.nombre}</div>}
                {selectedDate && <div className="flex items-center gap-2"><CalendarIcon size={16} /> {format(selectedDate, 'PPP', { locale: es })}</div>}
                {selectedTime && servicio && (
                  <div className="flex items-center gap-2 text-cyan-600 font-semibold">
                    <Clock size={16} /> 
                    {`${selectedTime} - ${format(addMinutes(parse(selectedTime, 'HH:mm', new Date()), servicio.duracionEnMinutos), 'HH:mm')}`}
                  </div>
                )}
              </div>
              <Button onClick={handleConfirmBooking} disabled={!selectedTime || isSubmitting} className="w-full mt-4 bg-cyan-600 hover:bg-cyan-700">
                {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <CheckCircle className="mr-2 h-4 w-4" />}
                Confirmar Reserva
              </Button>
            </CardContent>
          </Card>
        </div>

        {/* Columna de Pasos */}
        <div className="lg:col-span-2 space-y-8">
          {/* Paso 1: Seleccionar Profesional */}
          <Card className="shadow-md">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User /> Paso 1: Elige tu profesional
                {selectedTrabajador && <CheckCircle className="text-green-500" />}
              </CardTitle>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-4">
              {trabajadores.map(t => (
                <Button key={t.id} variant={selectedTrabajador?.id === t.id ? 'default' : 'outline'} onClick={() => handleSelectTrabajador(t)}>
                  {t.nombre}
                </Button>
              ))}
            </CardContent>
          </Card>

          {/* Paso 2: Seleccionar Fecha */}
          {selectedTrabajador && (
            <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
              <Card className="shadow-md">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <CalendarIcon /> Paso 2: Elige la fecha
                    {selectedDate && <CheckCircle className="text-green-500" />}
                  </CardTitle>
                </CardHeader>
                <CardContent className="flex justify-center">
                  <Calendar
                    mode="single"
                    selected={selectedDate}
                    onSelect={setSelectedDate}
                    disabled={[
                      ...diasNoDisponibles, // Días ocupados desde la API
                      { before: new Date() } // Todos los días antes de hoy
                    ]}
                    month={currentMonth}
                    onMonthChange={setCurrentMonth}
                    footer={loadingDias ? <p className="text-center text-sm text-gray-500 py-2">Actualizando calendario...</p> : null}
                    className="rounded-md border [cell-padding:0] w-full max-w-md"
                    locale={es}
                  />
                </CardContent>
              </Card>
            </motion.div>
          )}

          {/* Paso 3: Seleccionar Hora */}
          {selectedDate && (
            <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
              <Card className="shadow-md">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Clock /> Paso 3: Elige la hora
                    {selectedTime && <CheckCircle className="text-green-500" />}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {loadingHoras ? (
                    <div className="flex items-center gap-2 text-gray-500"><Loader2 className="h-5 w-5 animate-spin" /> Buscando horas...</div>
                  ) : horasDisponibles.length > 0 ? (
                    <div className="flex gap-3 overflow-x-auto pb-4">
                      {horasDisponibles.map(horaInicio => {
                        const displayTime = horaInicio;
                        return (
                          <Button key={horaInicio} variant={selectedTime === horaInicio ? 'default' : 'outline'} onClick={() => handleSelectTime(horaInicio)} className="flex-shrink-0">
                            {displayTime}
                          </Button>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="flex items-center gap-2 text-orange-600"><AlertCircle size={18} /> No hay horas disponibles para este día.</div>
                  )}
                </CardContent>
              </Card>
            </motion.div>
          )}
        </div>
      </div>
    </motion.div>
  );
}