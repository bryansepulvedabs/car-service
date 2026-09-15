-- =============================================
-- car-service: initial data
-- =============================================
INSERT INTO cars (license_plate, brand, model, year, color, category, fuel, seats, mileage, daily_rate, availability)
VALUES
    ('BBDF12', 'Toyota',     'Corolla',       2021, 'Blanco',  'SEDAN',     'GASOLINA', 5, 32000,  45000, true),
    ('GKRT45', 'Hyundai',    'Tucson',        2022, 'Gris',    'SUV',       'GASOLINA', 5, 18000,  65000, true),
    ('HJWQ89', 'Mazda',      'CX-5',          2023, 'Rojo',    'SUV',       'GASOLINA', 5,  8500,  72000, true),
    ('RLPN23', 'Suzuki',     'Swift',         2020, 'Azul',    'HATCHBACK', 'GASOLINA', 5, 47000,  35000, true),
    ('TMBV67', 'Kia',        'Picanto',       2022, 'Negro',   'CITYCAR',   'GASOLINA', 5, 21000,  28000, true),
    ('CVQJ34', 'BYD',        'Atto 3',        2024, 'Blanco',  'SUV',       'ELECTRICO',5,  3200,  85000, true),
    ('XNDK56', 'Toyota',     'Hilux',         2021, 'Plata',   'PICKUP',    'DIESEL',   5, 55000,  78000, true),
    ('FPLM90', 'Ford',       'Transit',       2020, 'Blanco',  'VAN',       'DIESEL',   9, 80000,  90000, true),
    ('QSZB11', 'Porsche',    'Boxster',       2022, 'Amarillo','DEPORTIVO', 'GASOLINA', 2,  5000, 150000, true),
    ('WJCR78', 'Toyota',     'Yaris Cross',   2023, 'Gris',    'HATCHBACK', 'HIBRIDO',  5, 12000,  55000, false);