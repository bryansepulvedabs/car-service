package rent_a_car_bryan.carservice.dto;

import lombok.Data;
import rent_a_car_bryan.carservice.entity.EnumCategory;
import rent_a_car_bryan.carservice.entity.EnumFuel;

@Data
public class CarResponseDTO {

    private Long id;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private EnumCategory category;
    private EnumFuel fuel;
    private Integer seats;
    private Integer mileage;
    private Boolean availability;
    private Long dailyRate;
    private String imageUrl;
    private String imagePhotographer;
    private String imagePhotographerUrl;
    private String imageSourceUrl;
}