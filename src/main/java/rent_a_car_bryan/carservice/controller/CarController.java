package rent_a_car_bryan.carservice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rent_a_car_bryan.carservice.entity.CarEntity;
import rent_a_car_bryan.carservice.service.CarService;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping
    public List<CarEntity> findAll(){
        return carService.findAll();
    }

    @GetMapping("/{id}")
    public CarEntity findById(@PathVariable Long id){
        return carService.findById(id);
    }

    @GetMapping("/plate/{licensePlate}")
    public CarEntity findByLicensePlate(@PathVariable String licensePlate){
        return carService.findByLicensePLate(licensePlate);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarEntity create(@RequestBody CarEntity car){
        return carService.save(car);
    }

    @PutMapping("/{id}")
    public CarEntity update(@PathVariable Long id, @RequestBody CarEntity carUpdate){
        return carService.update(id, carUpdate);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        carService.deleteById(id);
    }
}
