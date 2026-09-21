package rent_a_car_bryan.carservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rent_a_car_bryan.carservice.client.PexelsClient;
import rent_a_car_bryan.carservice.dto.CarRequestDTO;
import rent_a_car_bryan.carservice.dto.CarResponseDTO;
import rent_a_car_bryan.carservice.entity.CarEntity;
import rent_a_car_bryan.carservice.exception.ResourceNotFoundException;
import rent_a_car_bryan.carservice.repository.CarRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final PexelsClient pexelsClient;

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
                .orElseThrow(() -> new ResourceNotFoundException( "Auto no encontrado con patente: " + licensePlate));
        return toResponseDTO(car);
    }

    public CarResponseDTO save(CarRequestDTO carRequestDTO){
        CarEntity car = toEntity(carRequestDTO);
        car.setAvailability(true);
        assignImage(car);
        CarEntity savedCar = carRepository.save(car);
        return toResponseDTO(savedCar);
    }

    public CarResponseDTO update(Long id, CarRequestDTO carRequestDTO){
        CarEntity existentCar = findEntityById(id);
        boolean brandOrModelChanged =
                !existentCar.getBrand().equals(carRequestDTO.getBrand())
                        || !existentCar.getModel().equals(carRequestDTO.getModel());

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

        if (brandOrModelChanged) {
            assignImage(existentCar);
        }

        CarEntity updatedCar = carRepository.save(existentCar);
        return toResponseDTO(updatedCar);
    }

    public void deleteById(Long id){
        CarEntity car = findEntityById(id);
        carRepository.deleteById(car.getId());
    }

    private CarEntity findEntityById(Long id){
        return carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException( "Auto no encontrado con id : " + id));
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
        dto.setImageUrl(car.getImageUrl());
        dto.setImagePhotographer(car.getImagePhotographer());
        dto.setImagePhotographerUrl(car.getImagePhotographerUrl());
        dto.setImageSourceUrl(car.getImageSourceUrl());
        return dto;
    }

    public CarResponseDTO updateAvailability(Long id, boolean available) {
        CarEntity car = findEntityById(id);
        car.setAvailability(available);
        CarEntity updatedCar = carRepository.save(car);
        return toResponseDTO(updatedCar);
    }

    /**
     * Busca imagen en Pexels para todos los autos que aún no tienen una.
     * Devuelve cuántos autos quedaron con imagen.
     */
    public int refreshMissingImages() {
        List<CarEntity> withoutImage = carRepository.findAll()
                .stream()
                .filter(car -> car.getImageUrl() == null)
                .toList();

        int updated = 0;
        for (CarEntity car : withoutImage) {
            if (assignImage(car)) {
                carRepository.save(car);
                updated++;
            }
        }
        log.info("Imágenes asignadas: {} de {} autos sin imagen", updated, withoutImage.size());
        return updated;
    }

    // Devuelve true si encontró una foto y la asignó al auto
    private boolean assignImage(CarEntity car) {
        return pexelsClient.searchCarPhoto(car.getBrand(), car.getModel(), car.getYear())
                .map(photo -> {
                    car.setImageUrl(photo.src().landscape());
                    car.setImagePhotographer(photo.photographer());
                    car.setImagePhotographerUrl(photo.photographerUrl());
                    car.setImageSourceUrl(photo.url());
                    return true;
                })
                .orElse(false);
    }
}