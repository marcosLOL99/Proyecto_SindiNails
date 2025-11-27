import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { MapPin, Phone, Mail, Clock } from "lucide-react";
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
                    <p>Av. Elduayen, 16, 36380 Gondomar, Pontevedra</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Phone className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Teléfono</h3>
                    <p>+34 674 13 22 33</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Mail className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Email</h3>
                    <p>Sindinails2022@gmail.com</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Clock className="text-cyan-600 mt-1 flex-shrink-0" size={20} />
                  <div>
                    <h3 className="font-semibold text-gray-800">Horario</h3>
                    <p>Lunes a Jueves: 09:00 - 14:00 / 15:00 - 18:00</p>
                    <p>Viernes: 09:00 - 14:00 / 15:00 - 19:00</p>
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
            <div className="w-full h-full min-h-[450px] lg:min-h-full rounded-lg overflow-hidden shadow-lg border border-gray-200">
              <iframe
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d680.2150885159056!2d-8.764464530385565!3d42.11169479820107!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd258de6b9d72f49%3A0x805086b7d402a903!2sSindi%20Nails%20Beauty%20Studio!5e1!3m2!1ses!2ses!4v1764246718792!5m2!1ses!2ses"
                width="100%"
                height="100%"
                style={{ border: 0 }}
                allowFullScreen=""
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              ></iframe>
            </div>
          </motion.div>
        </div>
      </div>
    </motion.div>
  );
}