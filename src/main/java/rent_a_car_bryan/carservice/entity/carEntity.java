package rent_a_car_bryan.carservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cars")
@Data
public class carEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false, unique = true)
    private String patent;

    @Column (nullable = false)
    private String brand;

    @Column (nullable = false)
    private String model;

    @Column (nullable = false)
    private Integer year;

    private String color;

    @Column (nullable = false)
    private String category;

    @Column (nullable = false)
    private String fuel;

    @Column (nullable = false)
    private Integer seats;

    @Column (nullable = false)
    private Integer mileage;

    @Column (nullable = false)
    private String availability;






}
