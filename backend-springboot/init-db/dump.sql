-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 28-11-2025 a las 00:53:34
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `sindi_nails_db`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `citas`
--

CREATE TABLE `citas` (
  `id` bigint(20) NOT NULL,
  `estado` enum('CANCELADA','COMPLETADA','CONFIRMADA','PENDIENTE') DEFAULT NULL,
  `fecha_hora_fin` datetime(6) DEFAULT NULL,
  `fecha_hora_inicio` datetime(6) DEFAULT NULL,
  `cliente_id` bigint(20) NOT NULL,
  `servicio_id` bigint(20) NOT NULL,
  `trabajador_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `citas`
--

INSERT INTO `citas` (`id`, `estado`, `fecha_hora_fin`, `fecha_hora_inicio`, `cliente_id`, `servicio_id`, `trabajador_id`) VALUES
(4, 'CONFIRMADA', '2025-11-26 10:00:00.000000', '2025-11-26 09:00:00.000000', 2, 1, 3),
(7, 'CONFIRMADA', '2025-11-25 12:00:00.000000', '2025-11-25 11:00:00.000000', 4, 1, 1),
(8, 'CONFIRMADA', '2025-11-27 11:30:00.000000', '2025-11-27 10:30:00.000000', 4, 1, 1),
(9, 'CONFIRMADA', '2025-11-27 14:00:00.000000', '2025-11-27 13:00:00.000000', 2, 1, 1),
(10, 'CANCELADA', '2025-12-04 11:30:00.000000', '2025-12-04 11:00:00.000000', 2, 28, 1),
(11, 'CONFIRMADA', '2025-12-04 12:00:00.000000', '2025-12-04 11:00:00.000000', 2, 1, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `periodos_no_disponibles`
--

CREATE TABLE `periodos_no_disponibles` (
  `id` bigint(20) NOT NULL,
  `fecha_hora_fin` datetime(6) NOT NULL,
  `fecha_hora_inicio` datetime(6) NOT NULL,
  `motivo` varchar(255) NOT NULL,
  `tipo` enum('GLOBAL','TRABAJADOR') NOT NULL,
  `trabajador_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `periodos_no_disponibles`
--

INSERT INTO `periodos_no_disponibles` (`id`, `fecha_hora_fin`, `fecha_hora_inicio`, `motivo`, `tipo`, `trabajador_id`) VALUES
(2, '2025-12-10 20:00:00.000000', '2025-12-10 09:00:00.000000', 'Médico', 'TRABAJADOR', 2),
(4, '2025-12-12 12:00:00.000000', '2025-12-12 09:00:00.000000', 'Médico', 'TRABAJADOR', 3);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `servicio_local`
--

CREATE TABLE `servicio_local` (
  `id` bigint(20) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `duracion_en_minutos` int(11) NOT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `precio` double NOT NULL,
  `tipo_servicio_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `servicio_local`
--

INSERT INTO `servicio_local` (`id`, `descripcion`, `duracion_en_minutos`, `nombre`, `precio`, `tipo_servicio_id`) VALUES
(1, 'Corrige imperfecciones y fortalece la uña natural con base rubber para un acabado perfecto.', 60, 'Nivelación de uñas / Base Rubber', 25.5, 1),
(2, 'Manicura completa con esmalte de larga duración y secado inmediato.', 45, 'Esmaltado semipermanente', 20, 1),
(3, 'Cuidado clásico de uñas: limado, tratamiento de cutículas y esmaltado tradicional.', 35, 'Manicura tradicional', 14, 1),
(4, 'Eliminación cuidadosa y rápida del esmalte semipermanente, preparando la uña.', 10, 'Retirar semipermanente', 5, 1),
(5, 'Proceso seguro para retirar construcciones de uñas artificiales sin dañar la uña natural.', 15, 'Retirar uñas acrílicas / gel / acrigel', 9, 1),
(6, 'Servicio exprés de limado y esmaltado tradicional para un retoque rápido.', 15, 'Limar y pintar', 8, 1),
(7, 'Arreglo de una uña construida (acrílico o acrigel) que se haya roto o dañado.', 10, 'Reparación de uña acrílica / acrigel', 5, 1),
(8, 'Arreglo de una uña de gel que se haya roto o dañado.', 10, 'Reparación uña gel', 5, 1),
(9, 'Construcción de uñas artificiales con gel para mayor longitud y resistencia, con acabado natural.', 105, 'Uñas de gel', 39, 1),
(10, 'Mantenimiento de uñas de gel para cubrir el crecimiento natural y renovar el diseño.', 75, 'Relleno uñas de gel', 29, 1),
(11, 'Eliminación segura de la base rubber, protegiendo la uña natural.', 10, 'Retirar nivelación base Rubber', 6, 1),
(12, 'Construcción de uñas con acrílico o acrigel para una máxima durabilidad y longitud.', 105, 'Uñas acrílicas / acrigel', 39.5, 1),
(13, 'Mantenimiento de uñas de acrílico o acrigel para cubrir el crecimiento.', 75, 'Relleno acrílicas / acrigel', 29, 1),
(14, 'Tratamiento especializado para reconstruir y mejorar la apariencia de uñas mordidas.', 90, 'Reconstrucción uñas mordidas', 37, 1),
(15, 'Protocolo facial diseñado para reducir la hiperpigmentación y unificar el tono de la piel.', 60, 'Tratamiento facial antimanchas', 55.5, 3),
(16, 'Limpieza profunda para eliminar impurezas, puntos negros y regular el exceso de sebo.', 75, 'Limpieza facial purificante', 52, 3),
(17, 'Tratamiento intensivo con efecto tensor y regenerador para atenuar líneas de expresión.', 60, 'Tratamiento facial regenerador (efecto botox)', 55, 3),
(18, 'Tratamiento enfocado en mejorar la elasticidad y firmeza de la piel del rostro y cuello.', 60, 'Tratamiento facial reafirmante', 52.5, 3),
(19, 'Aporta luminosidad e hidratación intensiva a pieles apagadas o cansadas.', 60, 'Tratamiento facial revitalizante', 49.5, 3),
(20, 'Servicio exprés de limado y esmaltado tradicional para pies.', 20, 'Limar y pintar esmalte normal pies', 13, 2),
(21, 'Servicio exprés de limado y esmaltado semipermanente para pies.', 20, 'Limar y pintar semipermanente pies', 17, 2),
(22, 'Experiencia completa de pedicura con exfoliación, hidratación profunda y esmaltado tradicional.', 75, 'Pedicura spa esmalte normal', 35, 2),
(23, 'La pedicura spa completa con la durabilidad del esmaltado semipermanente.', 90, 'Pedicura spa esmalte semipermanente', 45, 2),
(24, 'Cuidado esencial de los pies: limado, cutículas, durezas leves y esmaltado tradicional.', 45, 'Pedicura tradicional', 22, 2),
(25, 'Pedicura completa con esmaltado semipermanente para un acabado perfecto y duradero.', 60, 'Pedicura semipermanente', 32, 2),
(26, 'Eliminación rápida y eficaz del vello visible de la nariz con cera.', 5, 'Depilación nariz (cera)', 4, 4),
(27, 'Diseño y depilación de cejas de alta precisión utilizando la técnica ancestral del hilo.', 15, 'Depilación cejas con hilo', 7.5, 4),
(28, 'Servicio completo de diseño con hilo más tinte para definir y potenciar la mirada.', 30, 'Depilación cejas con hilo y tinte', 13, 4),
(29, 'Diseño y depilación de cejas tradicional con cera.', 10, 'Depilación cejas con cera', 5.5, 4),
(30, 'Eliminación precisa del vello del labio superior con hilo.', 10, 'Depilación labio superior con hilo', 6.5, 4),
(31, 'Eliminación del vello del labio superior con cera.', 10, 'Depilación labio superior con cera', 5, 4),
(32, 'Depilación de precisión para la zona del labio superior y la barbilla con hilo.', 10, 'Depilación labio y barbilla con hilo', 9, 4),
(33, 'Depilación con cera de la zona del labio superior y la barbilla.', 10, 'Depilación labio y barbilla con cera', 7, 4),
(34, 'Aplicación de extensiones individuales, una a una, para un efecto natural y elegante.', 120, 'Extensiones de pestañas pelo a pelo (1ª puesta)', 65, 5),
(35, 'Aplicación de abanicos de extensiones ligeras para un efecto de mayor densidad y volumen.', 120, 'Extensiones de pestañas volumen (1ª puesta)', 75, 5),
(36, 'Mantenimiento de extensiones pelo a pelo tras 2 semanas desde la puesta.', 80, 'Relleno pestañas pelo a pelo (2 semanas)', 35, 5),
(37, 'Mantenimiento de extensiones pelo a pelo tras 3 semanas desde la puesta.', 95, 'Relleno pestañas pelo a pelo (3 semanas)', 45, 5),
(38, 'Mantenimiento de extensiones de volumen tras 2 semanas desde la puesta.', 85, 'Relleno pestañas volumen (2 semanas)', 38, 5),
(39, 'Mantenimiento de extensiones de volumen tras 3 semanas desde la puesta.', 95, 'Relleno pestañas volumen (3 semanas)', 48, 5),
(40, 'Tratamiento que riza y eleva la pestaña natural desde la raíz, abriendo la mirada.', 45, 'Lifting de pestañas', 29, 5),
(41, 'Lifting de pestañas combinado con tinte para oscurecerlas y potenciar el efecto.', 60, 'Lifting de pestañas con tinte', 35, 5),
(42, 'Aplicación de tinte específico para cejas, aportando color y definición.', 15, 'Tinte de cejas', 8, 6),
(43, 'Diseño y tinte de cejas con henna, que colorea el vello y sombrea la piel para un efecto maquillaje.', 45, 'Henna', 19, 6);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipo_servicio`
--

CREATE TABLE `tipo_servicio` (
  `id` int(11) NOT NULL,
  `nombre` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `tipo_servicio`
--

INSERT INTO `tipo_servicio` (`id`, `nombre`) VALUES
(6, 'Cejas'),
(4, 'Depilación'),
(1, 'Manicura'),
(2, 'Pedicura'),
(5, 'Pestañas'),
(3, 'Tratamiento Facial');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `trabajadores`
--

CREATE TABLE `trabajadores` (
  `id` bigint(20) NOT NULL,
  `activo` bit(1) NOT NULL,
  `apellidos` varchar(255) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `trabajadores`
--

INSERT INTO `trabajadores` (`id`, `activo`, `apellidos`, `nombre`) VALUES
(1, b'1', '', 'Sindia'),
(2, b'1', NULL, 'Layeska'),
(3, b'1', NULL, 'Lucia');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `turnos`
--

CREATE TABLE `turnos` (
  `id` bigint(20) NOT NULL,
  `fecha` date NOT NULL,
  `hora_fin` time(6) NOT NULL,
  `hora_inicio` time(6) NOT NULL,
  `trabajador_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `turnos`
--

INSERT INTO `turnos` (`id`, `fecha`, `hora_fin`, `hora_inicio`, `trabajador_id`) VALUES
(37, '2025-12-01', '14:00:00.000000', '10:00:00.000000', 1),
(38, '2025-12-01', '18:00:00.000000', '15:00:00.000000', 1),
(39, '2025-12-02', '14:00:00.000000', '10:00:00.000000', 1),
(40, '2025-12-02', '18:00:00.000000', '15:00:00.000000', 1),
(41, '2025-12-03', '14:00:00.000000', '10:00:00.000000', 1),
(42, '2025-12-03', '18:00:00.000000', '15:00:00.000000', 1),
(43, '2025-12-04', '14:00:00.000000', '10:00:00.000000', 1),
(44, '2025-12-04', '18:00:00.000000', '15:00:00.000000', 1),
(45, '2025-12-05', '14:00:00.000000', '10:00:00.000000', 1),
(46, '2025-12-05', '18:00:00.000000', '15:00:00.000000', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `id` bigint(20) NOT NULL,
  `apellidos` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','USER') DEFAULT NULL,
  `telefono` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`id`, `apellidos`, `email`, `nombre`, `password`, `role`, `telefono`) VALUES
(1, 'Usuario', 'nuevo.usuario@example.com', 'Nuevo', '$2a$10$genMOaEBJqHL6ERbjdsIEO6J1teVP3EJHFwbMarO7JEbQqarXZ68i', 'USER', '600123456'),
(2, 'González Flores', 'correomarcos@marcos.com', 'Marcos', '$2a$10$6MgXkNtOEOhmuoe5Bosi0uQ0nyf4/bXOO1YfJ5dJ2SYyc47SXhndm', 'USER', '604063719'),
(3, 'Pérez', 'prueba1@prueba.com', 'Prueba', '$2a$10$i.xphcHdvF.eNmChWttkGO3XXeL7iP4i5yREIcCFsWL2gMUVAwjJy', 'USER', '698098765'),
(4, 'Medina Fernández', 'carla.medinacmf14@gmail.com', 'Carla', '$2a$10$CSUeXD0I7.D4GKQ7f3bOZuybut1MSTTE9JE12aQYyeZ25g5eHAY3.', 'USER', '663509588'),
(5, 'Admin', 'admin@correo.com', 'Admin', '$2a$10$9ATx5ccTLe2l7X5xXphJeepKX6mOpgKDFn2z0ju9Qy.G1dFGqC.ia', 'ADMIN', '600100200');

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `citas`
--
ALTER TABLE `citas`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKcitrr49x1d6tcxpajkj7yj1mm` (`cliente_id`),
  ADD KEY `FKd3v5dokul8kxy4c67kwjk8bwh` (`servicio_id`),
  ADD KEY `FKmpyrctoll1n0n688uevrh2fe5` (`trabajador_id`);

--
-- Indices de la tabla `periodos_no_disponibles`
--
ALTER TABLE `periodos_no_disponibles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKrygvrv5x6p0bqrtageigvbucm` (`trabajador_id`);

--
-- Indices de la tabla `servicio_local`
--
ALTER TABLE `servicio_local`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_tipo_servicio` (`tipo_servicio_id`);

--
-- Indices de la tabla `tipo_servicio`
--
ALTER TABLE `tipo_servicio`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `nombre` (`nombre`);

--
-- Indices de la tabla `trabajadores`
--
ALTER TABLE `trabajadores`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `turnos`
--
ALTER TABLE `turnos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKhftxpmtlqmkydlhi61rf7vrh5` (`trabajador_id`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKkfsp0s1tflm1cwlj8idhqsad0` (`email`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `citas`
--
ALTER TABLE `citas`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT de la tabla `periodos_no_disponibles`
--
ALTER TABLE `periodos_no_disponibles`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `servicio_local`
--
ALTER TABLE `servicio_local`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=44;

--
-- AUTO_INCREMENT de la tabla `tipo_servicio`
--
ALTER TABLE `tipo_servicio`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `trabajadores`
--
ALTER TABLE `trabajadores`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `turnos`
--
ALTER TABLE `turnos`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=47;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `citas`
--
ALTER TABLE `citas`
  ADD CONSTRAINT `FKcitrr49x1d6tcxpajkj7yj1mm` FOREIGN KEY (`cliente_id`) REFERENCES `usuarios` (`id`),
  ADD CONSTRAINT `FKd3v5dokul8kxy4c67kwjk8bwh` FOREIGN KEY (`servicio_id`) REFERENCES `servicio_local` (`id`),
  ADD CONSTRAINT `FKmpyrctoll1n0n688uevrh2fe5` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`);

--
-- Filtros para la tabla `periodos_no_disponibles`
--
ALTER TABLE `periodos_no_disponibles`
  ADD CONSTRAINT `FKrygvrv5x6p0bqrtageigvbucm` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`);

--
-- Filtros para la tabla `servicio_local`
--
ALTER TABLE `servicio_local`
  ADD CONSTRAINT `fk_tipo_servicio` FOREIGN KEY (`tipo_servicio_id`) REFERENCES `tipo_servicio` (`id`);

--
-- Filtros para la tabla `turnos`
--
ALTER TABLE `turnos`
  ADD CONSTRAINT `FKhftxpmtlqmkydlhi61rf7vrh5` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
