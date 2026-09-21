package rent_a_car_bryan.carservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cars")
@Data
public class CarEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false, unique = true)
    private String licensePlate;

    @Column (nullable = false)
    private String brand;

    @Column (nullable = false)
    private String model;

    @Column (nullable = false)
    private Integer year;

    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumFuel fuel;

    @Column (nullable = false)
    private Integer seats;

    @Column (nullable = false)
    private Integer mileage;

    @Column (nullable = false)
    private Boolean availability;

    @Column(nullable = false)
    private Long dailyRate;

    // Imagen obtenida desde Pexels + datos de atribución
    @Column(length = 500)
    private String imageUrl;

    private String imagePhotographer;

    @Column(length = 500)
    private String imagePhotographerUrl;

    @Column(length = 500)
    private String imageSourceUrl;


}