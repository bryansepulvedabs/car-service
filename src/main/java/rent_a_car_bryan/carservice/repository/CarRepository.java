package rent_a_car_bryan.carservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rent_a_car_bryan.carservice.entity.CarEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<CarEntity, Long> {

    Optional<CarEntity> findByLicensePlate(String licensePlate);
    List<CarEntity> findAllByOrderByIdAsc();
}
