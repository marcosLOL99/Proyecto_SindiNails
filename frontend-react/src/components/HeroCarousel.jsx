import React from 'react';
import useEmblaCarousel from 'embla-carousel-react';
import Autoplay from 'embla-carousel-autoplay';
import './HeroCarousel.css';

import imagen1 from '../assets/imgCarrusel/imagen1.jpeg';
import imagen2 from '../assets/imgCarrusel/imagen2.jpeg';
import imagen3 from '../assets/imgCarrusel/imagen3.jpeg';
import imagen4 from '../assets/imgCarrusel/imagen4.jpeg';
import imagen5 from '../assets/imgCarrusel/imagen5.jpeg';
import imagen6 from '../assets/imgCarrusel/imagen6.jpeg';
import imagen7 from '../assets/imgCarrusel/imagen7.jpeg';

const images = [
  imagen1,
  imagen2,
  imagen3,
  imagen4,
  imagen5,
  imagen6,
  imagen7,
];

const HeroCarousel = () => {
  const [emblaRef] = useEmblaCarousel({ loop: true }, [Autoplay({ delay: 5000 })]);

  return (
    <div className="embla" ref={emblaRef}>
      <div className="embla__container">
        {images.map((src, index) => (
          <div className="embla__slide" key={index}>
            <img
              src={src}
              alt={`Fondo carrusel ${index + 1}`}
              className="embla__slide__img"
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default HeroCarousel;
