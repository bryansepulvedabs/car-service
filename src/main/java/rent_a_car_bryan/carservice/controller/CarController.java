package rent_a_car_bryan.carservice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rent_a_car_bryan.carservice.dto.CarRequestDTO;
import rent_a_car_bryan.carservice.dto.CarResponseDTO;
import rent_a_car_bryan.carservice.service.CarService;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping
    public List<CarResponseDTO> findAll(){
        return carService.findAll();
    }

    @GetMapping("/{id}")
    public CarResponseDTO findById(@PathVariable Long id){
        return carService.findById(id);
    }

    @GetMapping("/plate/{licensePlate}")
    public CarResponseDTO findByLicensePlate(@PathVariable String licensePlate){
        return carService.findByLicensePLate(licensePlate);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponseDTO create(@RequestBody CarRequestDTO car){
        return carService.save(car);
    }

    @PutMapping("/{id}")
    public CarResponseDTO update(@PathVariable Long id, @RequestBody CarRequestDTO carUpdate){
        return carService.update(id, carUpdate);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        carService.deleteById(id);
    }

    @PatchMapping("/{id}/availability")
    public CarResponseDTO updateAvailability(
            @PathVariable Long id,
            @RequestParam boolean available) {
        return carService.updateAvailability(id, available);
    }
}
