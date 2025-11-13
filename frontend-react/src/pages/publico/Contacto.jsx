import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { MapPin, Phone, Mail, Clock, Send } from "lucide-react";
import { motion } from "framer-motion";

export default function Contacto() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      className="bg-white text-gray-800 py-12 md:py-20 px-4"
    >
      <div className="container mx-auto">
        <div className="text-center mb-12">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-gray-900">
            Contacta con <span className="text-cyan-600">Nosotros</span>
          </h1>
          <p className="mt-4 max-w-2xl mx-auto text-lg text-gray-600">
            ¿Tienes alguna pregunta o quieres reservar tu cita? Estamos aquí para ayudarte.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Columna de Información */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, amount: 0.5 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <Card className="bg-gray-50 border-gray-200 shadow-lg h-full">
              <CardHeader>
                <CardTitle className="text-2xl text-gray-900">Información de Contacto</CardTitle>
              </CardHeader>
              <CardContent className="space-y-6 text-gray-600">
                <div className="flex items-start gap-4">
                  <MapPin className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Dirección</h3>
                    <p>Calle de la Belleza, 123, 28080 Madrid, España</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Phone className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Teléfono</h3>
                    <p>+34 912 345 678</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Mail className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Email</h3>
                    <p>hola@sindinails.com</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Clock className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Horario</h3>
                    <p>Lunes a Viernes: 09:00 - 14:00 / 15:00 - 18:00</p>
                    <p>Sábados: 10:00 - 14:00</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>

          {/* Columna del Formulario */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, amount: 0.5 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <Card className="bg-white border-gray-200 shadow-lg">
              <CardHeader>
                <CardTitle className="text-2xl text-gray-900">Envíanos un Mensaje</CardTitle>
              </CardHeader>
              <CardContent>
                <form className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <Input id="nombre" placeholder="Tu nombre" className="border-gray-300 focus:border-cyan-500 focus:ring-cyan-500" />
                    <Input id="email" type="email" placeholder="Tu email" className="border-gray-300 focus:border-cyan-500 focus:ring-cyan-500" />
                  </div>
                  <Input id="asunto" placeholder="Asunto" className="border-gray-300 focus:border-cyan-500 focus:ring-cyan-500" />
                  <Textarea id="mensaje" placeholder="Tu mensaje..." rows={5} className="border-gray-300 focus:border-cyan-500 focus:ring-cyan-500" />
                  <Button type="submit" className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold">
                    <Send className="mr-2 h-4 w-4" /> Enviar Mensaje
                  </Button>
                </form>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </motion.div>
  );
}