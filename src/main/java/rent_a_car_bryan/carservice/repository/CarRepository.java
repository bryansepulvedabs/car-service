package rent_a_car_bryan.carservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rent_a_car_bryan.carservice.entity.CarEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<CarEntity, Long> {

    Optional<CarEntity> findByLicensePlate(String licensePlate);
    List<CarEntity> findAllByOrderByIdAsc();

    // Consultas nativas: saltan @SQLRestriction, es la unica forma de ver los borrados.
    @Query(value = "SELECT * FROM cars WHERE deleted = true ORDER BY id", nativeQuery = true)
    List<CarEntity> findAllDeleted();

    @Query(value = "SELECT * FROM cars WHERE id = :id AND deleted = true", nativeQuery = true)
    Optional<CarEntity> findDeletedById(Long id);

    @Modifying
    @Query(value = "UPDATE cars SET deleted = false WHERE id = :id AND deleted = true", nativeQuery = true)
    int restoreById(Long id);
}