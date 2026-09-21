-- =============================================
-- car-service: initial data
-- =============================================
INSERT INTO cars (license_plate, brand, model, year, color, category, fuel, seats, mileage, daily_rate, availability,
                  image_url, image_photographer, image_photographer_url, image_source_url)
VALUES
    ('BBDF12', 'Toyota',     'Corolla',       2021, 'Blanco',  'SEDAN',     'GASOLINA', 5, 32000,  45000, true,
     'https://images.pexels.com/photos/16539605/pexels-photo-16539605.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Hoài  Nam', 'https://www.pexels.com/@hoinommm', 'https://www.pexels.com/photo/yellow-toyota-corolla-e70-on-driveway-16539605/'),
    ('GKRT45', 'Hyundai',    'Tucson',        2022, 'Gris',    'SUV',       'GASOLINA', 5, 18000,  65000, true,
     'https://images.pexels.com/photos/11245770/pexels-photo-11245770.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Hyundai Motor Group', 'https://www.pexels.com/@hyundaimotorgroup', 'https://www.pexels.com/photo/red-car-on-gray-asphalt-road-during-sunset-11245770/'),
    ('HJWQ89', 'Mazda',      'CX-5',          2023, 'Rojo',    'SUV',       'GASOLINA', 5,  8500,  72000, true,
     'https://images.pexels.com/photos/23409055/pexels-photo-23409055.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Dinesh Silwal', 'https://www.pexels.com/@dinesh-silwal-512732280', 'https://www.pexels.com/photo/cars-on-street-in-town-23409055/'),
    ('RLPN23', 'Suzuki',     'Swift',         2020, 'Azul',    'HATCHBACK', 'GASOLINA', 5, 47000,  35000, true,
     'https://images.pexels.com/photos/5556517/pexels-photo-5556517.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Diana ✨', 'https://www.pexels.com/@didsss', 'https://www.pexels.com/photo/modern-expensive-car-driving-fast-on-street-racing-5556517/'),
    ('TMBV67', 'Kia',        'Picanto',       2022, 'Negro',   'CITYCAR',   'GASOLINA', 5, 21000,  28000, true,
     'https://images.pexels.com/photos/20475132/pexels-photo-20475132.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Mike Bird', 'https://www.pexels.com/@mikebird', 'https://www.pexels.com/photo/white-kia-picanto-20475132/'),
    ('CVQJ34', 'BYD',        'Atto 3',        2024, 'Blanco',  'SUV',       'ELECTRICO',5,  3200,  85000, true,
     'https://images.pexels.com/photos/37822518/pexels-photo-37822518.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     '04iraq', 'https://www.pexels.com/@04iraq-1272398525', 'https://www.pexels.com/photo/white-car-on-scenic-road-in-shaqlawa-iraq-37822518/'),
    ('XNDK56', 'Toyota',     'Hilux',         2021, 'Plata',   'PICKUP',    'DIESEL',   5, 55000,  78000, true,
     'https://images.pexels.com/photos/39496385/pexels-photo-39496385.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Martijn Stoof', 'https://www.pexels.com/@martijn-stoof-2150654344', 'https://www.pexels.com/photo/dutch-police-vehicle-in-urban-street-scene-39496385/'),
    ('FPLM90', 'Ford',       'Transit',       2020, 'Blanco',  'VAN',       'DIESEL',   9, 80000,  90000, true,
     'https://images.pexels.com/photos/3310798/pexels-photo-3310798.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Muhammad-Taha Ibrahim', 'https://www.pexels.com/@planeteelevene', 'https://www.pexels.com/photo/driver-repairing-an-old-red-van-on-the-street-3310798/'),
    ('QSZB11', 'Porsche',    'Boxster',       2022, 'Amarillo','DEPORTIVO', 'GASOLINA', 2,  5000, 150000, true,
     'https://images.pexels.com/photos/37985719/pexels-photo-37985719.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Wolfgang Vrede', 'https://www.pexels.com/@wolfgang-vrede-9996223', 'https://www.pexels.com/photo/high-speed-car-on-iconic-nurburgring-track-37985719/'),
    ('WJCR78', 'Toyota',     'Yaris Cross',   2023, 'Gris',    'HATCHBACK', 'HIBRIDO',  5, 12000,  55000, false,
     'https://images.pexels.com/photos/24377246/pexels-photo-24377246.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=627&w=1200',
     'Alari Tammsalu', 'https://www.pexels.com/@alaritammsalu', 'https://www.pexels.com/photo/toyota-gr-yaris-speeding-through-the-snow-24377246/')
ON CONFLICT (license_plate) DO NOTHING;

-- Resetear la secuencia para que el próximo auto creado no choque con los ids manuales
SELECT setval('cars_id_seq', (SELECT COALESCE(MAX(id), 0) FROM cars));