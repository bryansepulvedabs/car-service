package rent_a_car_bryan.carservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "cars")
@Data
// Borrado logico: carRepository.deleteById() se traduce a un UPDATE, nunca a un DELETE.
@SQLDelete(sql = "UPDATE cars SET deleted = true WHERE id = ?")
// Y todas las consultas filtran los borrados: un auto dado de baja desaparece del
// catalogo, de findByLicensePlate() y de refreshMissingImages() sin tocar el servicio.
@SQLRestriction("deleted = false")
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

    // Ojo: esto es "operativo / fuera de servicio" (mantencion), lo cambia el admin.
    // No tiene nada que ver con estar arrendado: eso depende de las fechas y lo
    // resuelve rental-service.
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

    // columnDefinition con DEFAULT: sin el, ddl-auto=update falla al agregar una
    // columna NOT NULL sobre una tabla que ya tiene filas.
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private Boolean deleted = false;

}