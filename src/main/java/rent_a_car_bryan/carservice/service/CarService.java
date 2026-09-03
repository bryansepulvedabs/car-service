package rent_a_car_bryan.carservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rent_a_car_bryan.carservice.dto.CarRequestDTO;
import rent_a_car_bryan.carservice.dto.CarResponseDTO;
import rent_a_car_bryan.carservice.entity.CarEntity;
import rent_a_car_bryan.carservice.repository.CarRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    public List<CarResponseDTO> findAll (){
        return carRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CarResponseDTO findById(Long id){
        CarEntity car = findEntityById(id);
        return toResponseDTO(car);
    }

    public CarResponseDTO findByLicensePLate (String licensePlate){
        CarEntity car = carRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new RuntimeException( "Auto no encontrado con patente: " + licensePlate));
        return toResponseDTO(car);
    }

    public CarResponseDTO save(CarRequestDTO carRequestDTO){
        CarEntity car = toEntity(carRequestDTO);
        car.setAvailability(true);
        CarEntity savedCar = carRepository.save(car);
        return toResponseDTO(savedCar);
    }

    public CarResponseDTO update(Long id, CarRequestDTO carRequestDTO){
        CarEntity existentCar = findEntityById(id);

        existentCar.setLicensePlate(carRequestDTO.getLicensePlate());
        existentCar.setBrand(carRequestDTO.getBrand());
        existentCar.setModel(carRequestDTO.getModel());
        existentCar.setYear(carRequestDTO.getYear());
        existentCar.setColor(carRequestDTO.getColor());
        existentCar.setCategory(carRequestDTO.getCategory());
        existentCar.setFuel(carRequestDTO.getFuel());
        existentCar.setSeats(carRequestDTO.getSeats());
        existentCar.setMileage(carRequestDTO.getMileage());
        existentCar.setDailyRate(carRequestDTO.getDailyRate());

        CarEntity updatedCar = carRepository.save(existentCar);
        return toResponseDTO(updatedCar);
    }

    public void deleteById(Long id){
        CarEntity car = findEntityById(id);
        carRepository.deleteById(car.getId());
    }

    private CarEntity findEntityById(Long id){
        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException( "Auto no encontrado con id : " + id));
    }

    private CarEntity toEntity(CarRequestDTO dto){
        CarEntity car = new CarEntity();
        car.setLicensePlate(dto.getLicensePlate());
        car.setBrand(dto.getBrand());
        car.setModel(dto.getModel());
        car.setYear(dto.getYear());
        car.setColor(dto.getColor());
        car.setCategory(dto.getCategory());
        car.setFuel(dto.getFuel());
        car.setSeats(dto.getSeats());
        car.setMileage(dto.getMileage());
        car.setDailyRate(dto.getDailyRate());
        return car;
    }


    private CarResponseDTO toResponseDTO(CarEntity car){
        CarResponseDTO dto = new CarResponseDTO();
        dto.setId(car.getId());
        dto.setLicensePlate(car.getLicensePlate());
        dto.setBrand(car.getBrand());
        dto.setModel(car.getModel());
        dto.setYear(car.getYear());
        dto.setColor(car.getColor());
        dto.setCategory(car.getCategory());
        dto.setFuel(car.getFuel());
        dto.setSeats(car.getSeats());
        dto.setMileage(car.getMileage());
        dto.setAvailability(car.getAvailability());
        dto.setDailyRate(car.getDailyRate());
        return dto;
    }

    public CarResponseDTO updateAvailability(Long id, boolean available) {
        CarEntity car = findEntityById(id);
        car.setAvailability(available);
        CarEntity updatedCar = carRepository.save(car);
        return toResponseDTO(updatedCar);
    }
}
