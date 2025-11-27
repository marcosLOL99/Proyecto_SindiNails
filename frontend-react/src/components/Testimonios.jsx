import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Star, MessageSquare } from 'lucide-react';

const testimoniosData = [
  {
    id: 1,
    nombre: "Claudia M",
    texto: "Es mi sitio preferido para hacerme las uñas y eso que he ido a muchísimos. Lucia es una profesional de diez, aunque todas lo hacen muy bien. Utilizan productos de calidad y no te estropean las uñas. No dudéis en venir aquí!",
    rating: 5,
  },
  {
    id: 2,
    nombre: "Eva González",
    texto: "No es que sea 100% recomendable, es que es un 1000%! Sindi capta a la perfección la idea que tienes en la cabeza y la lleva a otro nivel. Gracias a ella, voy feliz con la mejor manicura y pedicura de novia que nunca soñé 😊",
    rating: 5,
  },
  {
    id: 3,
    nombre: "nohelia ines flores baez",
    texto: "Estoy encantada, muy buena profesional, me gusta la puntualidad, los productos son de lo mejor, el trabajo impecable y el trato inmejorable super recomendable!!!",
    rating: 5,
  },
];

const StarRating = ({ rating }) => (
  <div className="flex items-center gap-1">
    {[...Array(5)].map((_, i) => (
      <Star
        key={i}
        size={16}
        className={i < rating ? "text-yellow-400 fill-yellow-400" : "text-gray-300"}
      />
    ))}
  </div>
);

const Testimonios = () => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      {testimoniosData.map((testimonio) => (
        <Card key={testimonio.id} className="bg-white border-gray-200 flex flex-col shadow-lg hover:shadow-xl transition-shadow duration-300 h-full">
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle className="text-lg text-gray-900">{testimonio.nombre}</CardTitle>
              <StarRating rating={testimonio.rating} />
            </div>
          </CardHeader>
          <CardContent className="flex-grow">
            <blockquote className="text-gray-600 italic border-l-4 border-cyan-200 pl-4">
              "{testimonio.texto}"
            </blockquote>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};

export default Testimonios;

