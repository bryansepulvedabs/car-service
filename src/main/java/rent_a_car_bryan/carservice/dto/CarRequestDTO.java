package rent_a_car_bryan.carservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import rent_a_car_bryan.carservice.entity.EnumCategory;
import rent_a_car_bryan.carservice.entity.EnumFuel;

@Data
public class CarRequestDTO {

    @NotBlank(message = "La patente es obligatoria")
    @Size(min = 5, max = 10, message = "La patente debe tener entre 5 y 10 caracteres")
    private String licensePlate;

    @NotBlank(message = "La marca es obligatoria")
    private String brand;

    @NotBlank(message = "El modelo es obligatorio")
    private String model;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 1980, message = "El año no puede ser anterior a 1980")
    @Max(value = 2100, message = "El año no es válido")
    private Integer year;

    private String color;

    @NotNull(message = "La categoría es obligatoria")
    private EnumCategory category;

    @NotNull(message = "El combustible es obligatorio")
    private EnumFuel fuel;

    @NotNull(message = "Los asientos son obligatorios")
    @Min(value = 1, message = "El auto debe tener al menos 1 asiento")
    @Max(value = 60, message = "La cantidad de asientos no es válida")
    private Integer seats;

    @NotNull(message = "El kilometraje es obligatorio")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Integer mileage;

    @NotNull(message = "La tarifa diaria es obligatoria")
    @Positive(message = "La tarifa diaria debe ser mayor a 0")
    private Long dailyRate;
}