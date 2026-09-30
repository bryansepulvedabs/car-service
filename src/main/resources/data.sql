-- car-service/src/main/resources/data.sql
--
-- Ids explicitos: rental-service guarda car_id apuntando a esta base, y como son
-- bases separadas los ids tienen que ser deterministas.
-- ON CONFLICT DO NOTHING (sin target) cubre tanto la PK como license_plate, para no
-- pisar cambios hechos en runtime (imagenes de Pexels, kilometraje, tarifas).
--
-- availability ya NO la toca rental-service: significa "operativo / fuera de servicio"
-- y la cambia el admin a mano. El auto 10 va en false para probar ese caso.

INSERT INTO cars (id, license_plate, brand, model, year, color, category, fuel, seats, mileage, availability, daily_rate) VALUES
                                                                                                                              (1,  'BBDF12', 'Toyota',    'Corolla',  2022, 'Blanco', 'SEDAN',     'GASOLINA',  5, 45200, true,  45000),
                                                                                                                              (2,  'CDFG34', 'Hyundai',   'Tucson',   2023, 'Gris',   'SUV',       'DIESEL',    5, 32800, true,  62000),
                                                                                                                              (3,  'DFGH56', 'Chevrolet', 'Sail',     2021, 'Rojo',   'SEDAN',     'GASOLINA',  5, 68400, true,  32000),
                                                                                                                              (4,  'FGHJ78', 'Kia',       'Morning',  2021, 'Blanco', 'CITYCAR',   'GASOLINA',  5, 74100, true,  25000),
                                                                                                                              (5,  'GHJK90', 'Toyota',    'Hilux',    2023, 'Negro',  'PICKUP',    'DIESEL',    5, 28600, true,  78000),
                                                                                                                              (6,  'HJKL12', 'Mazda',     'CX-5',     2024, 'Gris',   'SUV',       'GASOLINA',  5, 12300, true,  72000),
                                                                                                                              (7,  'JKLM34', 'Hyundai',   'Staria',   2023, 'Plata',  'VAN',       'DIESEL',    8, 39700, true,  85000),
                                                                                                                              (8,  'KLMN56', 'Tesla',     'Model 3',  2024, 'Blanco', 'SEDAN',     'ELECTRICO', 5,  9400, true,  95000),
                                                                                                                              (9,  'LMNP78', 'Toyota',    'Prius',    2022, 'Verde',  'HATCHBACK', 'HIBRIDO',   5, 41500, true,  52000),
                                                                                                                              (10, 'MNPQ90', 'Subaru',    'BRZ',      2023, 'Azul',   'DEPORTIVO', 'GASOLINA',  4, 21900, false, 88000)
ON CONFLICT DO NOTHING;

-- Deja la secuencia despues del ultimo id insertado a mano, si no el proximo
-- INSERT desde la API choca con la PK.
SELECT setval(pg_get_serial_sequence('cars', 'id'), COALESCE((SELECT MAX(id) FROM cars), 1));