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

    public CarEntity findById(Long id){
        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException( "Auto no encontrado con id : " + id));
    }

    public CarEntity findByLicensePLate (String licensePlate){
        return carRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new RuntimeException( "Auto no encontrado con patente: " + licensePlate));
    }

    public CarEntity save(CarEntity car){
        return carRepository.save(car);
    }

    public CarEntity update(Long id, CarEntity carUpdate){
        CarEntity existentCar = findById(id);

        existentCar.setLicensePlate(carUpdate.getLicensePlate());
        existentCar.setBrand(carUpdate.getBrand());
        existentCar.setModel(carUpdate.getModel());
        existentCar.setYear(carUpdate.getYear());
        existentCar.setColor(carUpdate.getColor());
        existentCar.setCategory(carUpdate.getCategory());
        existentCar.setFuel(carUpdate.getFuel());
        existentCar.setSeats(carUpdate.getSeats());
        existentCar.setMileage(carUpdate.getMileage());
        existentCar.setAvailability(carUpdate.getAvailability());
        existentCar.setDailyRate(carUpdate.getDailyRate());

        return carRepository.save(carUpdate);
    }

    public void deleteById(Long id){
        CarEntity car = findById(id);
        carRepository.deleteById(car.getId());
    }
}
