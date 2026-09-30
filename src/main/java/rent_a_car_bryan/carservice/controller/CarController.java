package rent_a_car_bryan.carservice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rent_a_car_bryan.carservice.dto.CarRequestDTO;
import rent_a_car_bryan.carservice.dto.CarResponseDTO;
import rent_a_car_bryan.carservice.service.CarService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping
    public List<CarResponseDTO> findAll(){
        return carService.findAll();
    }

    // Autos dados de baja: solo ADMIN (ver las reglas que hay que agregar al SecurityConfig de car-service)
    @GetMapping("/deleted")
    public List<CarResponseDTO> findAllDeleted(){
        return carService.findAllDeleted();
    }

    @GetMapping("/{id}")
    public CarResponseDTO findById(@PathVariable Long id){
        return carService.findById(id);
    }

    // Ficha incluyendo eliminados, para revisar su historial. ADMIN, o rental-service
    // (rol SERVICE) al armar el historial de un arriendo cuyo auto fue dado de baja.
    @GetMapping("/admin/{id}")
    public CarResponseDTO findByIdIncludingDeleted(@PathVariable Long id){
        return carService.findByIdIncludingDeleted(id);
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

    // Reactivar un auto dado de baja: solo ADMIN
    @PatchMapping("/{id}/restore")
    public CarResponseDTO restore(@PathVariable Long id){
        return carService.restore(id);
    }

    // Rellena con fotos de Pexels los autos que no tienen imagen (por ejemplo, los del data.sql)
    @PostMapping("/images/refresh")
    public Map<String, Integer> refreshImages() {
        return Map.of("updated", carService.refreshMissingImages());
    }
}