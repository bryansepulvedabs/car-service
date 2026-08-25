package rent_a_car_bryan.carservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rent_a_car_bryan.carservice.entity.CarEntity;
import rent_a_car_bryan.carservice.repository.CarRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    public List<CarEntity> findAll (){
        return carRepository.findAll();
    }

    public CarEntity findByLicensePLate (String licensePlate){
        return carRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new RuntimeException( "Auto no encontrado con patente: " + licensePlate));
    }

    public CarEntity save(CarEntity car){
        return carRepository.save(car);
    }
}
