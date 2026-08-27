package rent_a_car_bryan.carservice.dto;

import lombok.Data;
import rent_a_car_bryan.carservice.entity.Categoria;
import rent_a_car_bryan.carservice.entity.Combustible;

@Data
public class CarRequestDTO {
    private String licensePlate;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private Categoria category;
    private Combustible fuel;
    private Integer seats;
    private Integer mileage;
    private Long dailyRate;
}
