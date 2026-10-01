package rent_a_car_bryan.carservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import rent_a_car_bryan.carservice.client.PexelsClient;
import rent_a_car_bryan.carservice.client.PexelsSearchResponse.PexelsPhoto;
import rent_a_car_bryan.carservice.client.PexelsSearchResponse.PexelsSrc;
import rent_a_car_bryan.carservice.dto.CarRequestDTO;
import rent_a_car_bryan.carservice.dto.CarResponseDTO;
import rent_a_car_bryan.carservice.entity.CarEntity;
import rent_a_car_bryan.carservice.entity.EnumCategory;
import rent_a_car_bryan.carservice.entity.EnumFuel;
import rent_a_car_bryan.carservice.exception.InvalidCarOperationException;
import rent_a_car_bryan.carservice.exception.ResourceNotFoundException;
import rent_a_car_bryan.carservice.repository.CarRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de la lógica de CarService. Sin base de datos ni red: el repositorio y el
 * cliente de Pexels son mocks.
 *
 * Qué NO cubren (a propósito): el borrado lógico real (@SQLDelete / @SQLRestriction) y la
 * restricción unique de la patente ocurren en la base de datos; se prueban contra PostgreSQL
 * en la ronda de integración.
 */
@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    private static final long CAR_ID = 5L;

    @Mock private CarRepository carRepository;
    @Mock private PexelsClient pexelsClient;

    @InjectMocks private CarService service;

    @BeforeEach
    void setUp() {
        // save() devuelve lo mismo que recibe, como haría JPA
        lenient().when(carRepository.save(any(CarEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        // Por defecto Pexels no encuentra nada; cada test que lo necesita lo sobreescribe
        lenient().when(pexelsClient.searchCarPhoto(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    // =====================================================================================
    // Crear
    // =====================================================================================
    @Nested
    @DisplayName("Crear auto")
    class Create {

        @Test
        @DisplayName("el auto nuevo queda operativo (availability = true) y copia los datos del request")
        void newCarStartsOperational() {
            CarResponseDTO result = service.save(carRequest());

            ArgumentCaptor<CarEntity> saved = ArgumentCaptor.forClass(CarEntity.class);
            verify(carRepository).save(saved.capture());
            CarEntity entity = saved.getValue();
            assertThat(entity.getAvailability()).isTrue();
            assertThat(entity.getLicensePlate()).isEqualTo("ABCD12");
            assertThat(entity.getBrand()).isEqualTo("Toyota");
            assertThat(entity.getModel()).isEqualTo("Yaris");
            assertThat(entity.getYear()).isEqualTo(2023);
            assertThat(entity.getCategory()).isEqualTo(EnumCategory.SEDAN);
            assertThat(entity.getFuel()).isEqualTo(EnumFuel.GASOLINA);
            assertThat(entity.getSeats()).isEqualTo(5);
            assertThat(entity.getMileage()).isEqualTo(10_000);
            assertThat(entity.getDailyRate()).isEqualTo(30_000L);
            assertThat(result.getAvailability()).isTrue();
            assertThat(result.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("busca la foto con marca, modelo, año y color del auto")
        void searchesPhotoWithCarData() {
            service.save(carRequest());

            verify(pexelsClient).searchCarPhoto("Toyota", "Yaris", 2023, "Blanco");
        }

        @Test
        @DisplayName("si Pexels encuentra una foto, queda guardada con su atribución")
        void storesPhotoWhenFound() {
            when(pexelsClient.searchCarPhoto("Toyota", "Yaris", 2023, "Blanco")).thenReturn(Optional.of(photo()));

            CarResponseDTO result = service.save(carRequest());

            assertThat(result.getImageUrl()).isEqualTo("https://img.pexels.com/landscape.jpg");
            assertThat(result.getImagePhotographer()).isEqualTo("Ana");
            assertThat(result.getImagePhotographerUrl()).isEqualTo("https://www.pexels.com/@ana");
            assertThat(result.getImageSourceUrl()).isEqualTo("https://www.pexels.com/photo/1");
        }

        @Test
        @DisplayName("si Pexels no encuentra foto (o falla), el auto se guarda igual, sin imagen")
        void savesEvenWithoutPhoto() {
            CarResponseDTO result = service.save(carRequest());

            verify(carRepository).save(any(CarEntity.class));
            assertThat(result.getImageUrl()).isNull();
        }
    }

    // =====================================================================================
    // Editar
    // =====================================================================================
    @Nested
    @DisplayName("Editar auto")
    class Update {

        @ParameterizedTest(name = "nueva marca = {0}, nuevo modelo = {1}")
        @CsvSource({"Honda,Yaris", "Toyota,Corolla"})
        @DisplayName("si cambia la marca o el modelo, se vuelve a buscar la foto")
        void refreshesPhotoWhenBrandOrModelChanges(String brand, String model) {
            givenCar(carWith(CAR_ID, "Yaris", "https://old.jpg"));
            when(pexelsClient.searchCarPhoto(eq(brand), eq(model), any(), any())).thenReturn(Optional.of(photo()));
            CarRequestDTO request = carRequest();
            request.setBrand(brand);
            request.setModel(model);

            CarResponseDTO result = service.update(CAR_ID, request);

            verify(pexelsClient).searchCarPhoto(eq(brand), eq(model), any(), any());
            assertThat(result.getImageUrl()).isEqualTo("https://img.pexels.com/landscape.jpg");
        }

        @Test
        @DisplayName("si cambian otros datos (tarifa, kilometraje, color…), NO se vuelve a buscar la foto")
        void doesNotRefreshPhotoForOtherChanges() {
            CarEntity existing = carWith(CAR_ID, "Yaris", "https://old.jpg");
            givenCar(existing);
            CarRequestDTO request = carRequest();
            request.setDailyRate(45_000L);
            request.setColor("Negro");

            CarResponseDTO result = service.update(CAR_ID, request);

            verifyNoInteractions(pexelsClient);
            assertThat(result.getImageUrl()).isEqualTo("https://old.jpg");
            assertThat(result.getDailyRate()).isEqualTo(45_000L);
            assertThat(result.getColor()).isEqualTo("Negro");
        }

        @Test
        @DisplayName("editar los datos no cambia el estado operativo: eso solo lo hace updateAvailability")
        void updateDoesNotTouchAvailability() {
            CarEntity existing = carWith(CAR_ID, "Yaris", null);
            existing.setAvailability(false); // en mantención
            givenCar(existing);

            CarResponseDTO result = service.update(CAR_ID, carRequest());

            assertThat(result.getAvailability()).isFalse();
        }

        @Test
        @DisplayName("editar un auto inexistente responde 404")
        void updateNotFound() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(CAR_ID, carRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Auto no encontrado");

            verify(carRepository, never()).save(any());
        }
    }

    // =====================================================================================
    // Kilometraje
    // =====================================================================================
    @Nested
    @DisplayName("Kilometraje")
    class Mileage {

        @Test
        @DisplayName("un kilometraje mayor al actual se guarda")
        void increasesMileage() {
            givenAnyCar(carWith(CAR_ID, "Yaris", null)); // 10.000 km

            CarResponseDTO result = service.updateMileage(CAR_ID, 12_000);

            assertThat(result.getMileage()).isEqualTo(12_000);
            verify(carRepository).save(any(CarEntity.class));
        }

        @Test
        @DisplayName("un kilometraje igual al actual se acepta")
        void acceptsSameMileage() {
            givenAnyCar(carWith(CAR_ID, "Yaris", null));

            CarResponseDTO result = service.updateMileage(CAR_ID, 10_000);

            assertThat(result.getMileage()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("el kilometraje nunca baja: uno menor al actual se rechaza y no se guarda")
        void neverDecreases() {
            givenAnyCar(carWith(CAR_ID, "Yaris", null));

            assertThatThrownBy(() -> service.updateMileage(CAR_ID, 9_999))
                    .isInstanceOf(InvalidCarOperationException.class)
                    .hasMessageContaining("no puede ser menor al actual")
                    .hasMessageContaining("10000");

            verify(carRepository, never()).save(any());
        }

        @Test
        @DisplayName("un kilometraje negativo se rechaza")
        void rejectsNegative() {
            givenAnyCar(carWith(CAR_ID, "Yaris", null));

            assertThatThrownBy(() -> service.updateMileage(CAR_ID, -1))
                    .isInstanceOf(InvalidCarOperationException.class)
                    .hasMessageContaining("negativo");

            verify(carRepository, never()).save(any());
        }

        @Test
        @DisplayName("funciona también con un auto dado de baja (usa findAnyById, no findById)")
        void worksForDeletedCars() {
            CarEntity deleted = carWith(CAR_ID, "Yaris", null);
            deleted.setDeleted(true);
            givenAnyCar(deleted);

            CarResponseDTO result = service.updateMileage(CAR_ID, 11_000);

            assertThat(result.getMileage()).isEqualTo(11_000);
            verify(carRepository, never()).findById(any());
        }

        @Test
        @DisplayName("un auto que no existe responde 404")
        void notFound() {
            when(carRepository.findAnyById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMileage(CAR_ID, 12_000))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =====================================================================================
    // Disponibilidad operativa
    // =====================================================================================
    @Nested
    @DisplayName("Disponibilidad (operativo / mantención)")
    class Availability {

        @ParameterizedTest(name = "available = {0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("cambia solo el estado operativo")
        void changesOnlyAvailability(boolean available) {
            CarEntity existing = carWith(CAR_ID, "Yaris", "https://old.jpg");
            existing.setAvailability(!available);
            givenCar(existing);

            CarResponseDTO result = service.updateAvailability(CAR_ID, available);

            assertThat(result.getAvailability()).isEqualTo(available);
            assertThat(result.getMileage()).isEqualTo(10_000);
            assertThat(result.getDailyRate()).isEqualTo(30_000L);
            verifyNoInteractions(pexelsClient);
        }

        @Test
        @DisplayName("un auto inexistente responde 404")
        void notFound() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAvailability(CAR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =====================================================================================
    // Eliminar y reactivar
    // =====================================================================================
    @Nested
    @DisplayName("Eliminar y reactivar")
    class DeleteAndRestore {

        @Test
        @DisplayName("eliminar un auto inexistente responde 404")
        void deleteNotFound() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteById(CAR_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(carRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("eliminar delega en el repositorio (el borrado lógico lo hace la entidad)")
        void deleteDelegates() {
            givenCar(carWith(CAR_ID, "Yaris", null));

            service.deleteById(CAR_ID);

            verify(carRepository).deleteById(CAR_ID);
        }

        @Test
        @DisplayName("reactivar un auto que no estaba eliminado responde 404")
        void restoreNotFound() {
            when(carRepository.findDeletedById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.restore(CAR_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("no estaba dado de baja");

            verify(carRepository, never()).restoreById(any());
        }

        @Test
        @DisplayName("reactivar devuelve el auto con deleted = false")
        void restoreReturnsActiveCar() {
            CarEntity deleted = carWith(CAR_ID, "Yaris", null);
            deleted.setDeleted(true);
            when(carRepository.findDeletedById(CAR_ID)).thenReturn(Optional.of(deleted));

            CarResponseDTO result = service.restore(CAR_ID);

            verify(carRepository).restoreById(CAR_ID);
            assertThat(result.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("si la patente fue reasignada mientras estaba de baja, el error de integridad sube (el handler lo hace 409)")
        void restoreConflictPropagates() {
            CarEntity deleted = carWith(CAR_ID, "Yaris", null);
            deleted.setDeleted(true);
            when(carRepository.findDeletedById(CAR_ID)).thenReturn(Optional.of(deleted));
            when(carRepository.restoreById(CAR_ID)).thenThrow(new DataIntegrityViolationException("duplicate key"));

            assertThatThrownBy(() -> service.restore(CAR_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // =====================================================================================
    // Lectura
    // =====================================================================================
    @Nested
    @DisplayName("Lectura")
    class Reads {

        @Test
        @DisplayName("el catálogo se pide ordenado por id")
        void findAllIsOrdered() {
            when(carRepository.findAllByOrderByIdAsc()).thenReturn(
                    List.of(carWith(1L, "Yaris", null), carWith(2L, "Corolla", null)));

            assertThat(service.findAll()).extracting(CarResponseDTO::getModel)
                    .containsExactly("Yaris", "Corolla");
        }

        @Test
        @DisplayName("la lista de eliminados marca deleted = true")
        void findAllDeletedMarksDeleted() {
            CarEntity deleted = carWith(CAR_ID, "Yaris", null);
            deleted.setDeleted(true);
            when(carRepository.findAllDeleted()).thenReturn(List.of(deleted));

            assertThat(service.findAllDeleted()).singleElement()
                    .extracting(CarResponseDTO::getDeleted).isEqualTo(true);
        }

        @Test
        @DisplayName("findById devuelve todos los datos, con la atribución de la foto")
        void findByIdMapsEveryField() {
            CarEntity car = carWith(CAR_ID, "Yaris", "https://img.jpg");
            car.setImagePhotographer("Ana");
            car.setImagePhotographerUrl("https://www.pexels.com/@ana");
            car.setImageSourceUrl("https://www.pexels.com/photo/1");
            givenCar(car);

            CarResponseDTO dto = service.findById(CAR_ID);

            assertThat(dto.getId()).isEqualTo(CAR_ID);
            assertThat(dto.getLicensePlate()).isEqualTo("ABCD12");
            assertThat(dto.getCategory()).isEqualTo(EnumCategory.SEDAN);
            assertThat(dto.getFuel()).isEqualTo(EnumFuel.GASOLINA);
            assertThat(dto.getImagePhotographer()).isEqualTo("Ana");
            assertThat(dto.getImageSourceUrl()).isEqualTo("https://www.pexels.com/photo/1");
            assertThat(dto.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("un deleted nulo en la entidad se muestra como false")
        void nullDeletedIsFalse() {
            CarEntity car = carWith(CAR_ID, "Yaris", null);
            car.setDeleted(null);
            givenCar(car);

            assertThat(service.findById(CAR_ID).getDeleted()).isFalse();
        }

        @Test
        @DisplayName("findById de un auto inexistente (o dado de baja) responde 404 con el id")
        void findByIdNotFound() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(CAR_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("5");
        }

        @Test
        @DisplayName("la ficha de admin incluye eliminados y lo marca")
        void adminDetailIncludesDeleted() {
            CarEntity deleted = carWith(CAR_ID, "Yaris", null);
            deleted.setDeleted(true);
            givenAnyCar(deleted);

            assertThat(service.findByIdIncludingDeleted(CAR_ID).getDeleted()).isTrue();
        }

        @Test
        @DisplayName("la ficha de admin responde 404 si el auto no existe ni eliminado")
        void adminDetailNotFound() {
            when(carRepository.findAnyById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByIdIncludingDeleted(CAR_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("buscar por patente devuelve el auto")
        void findByLicensePlate() {
            when(carRepository.findByLicensePlate("ABCD12")).thenReturn(Optional.of(carWith(CAR_ID, "Yaris", null)));

            assertThat(service.findByLicensePLate("ABCD12").getId()).isEqualTo(CAR_ID);
        }

        @Test
        @DisplayName("buscar por una patente inexistente responde 404 con la patente")
        void findByLicensePlateNotFound() {
            when(carRepository.findByLicensePlate("ZZZZ99")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByLicensePLate("ZZZZ99"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ZZZZ99");
        }
    }

    // =====================================================================================
    // Refrescar imágenes
    // =====================================================================================
    @Nested
    @DisplayName("Refrescar imágenes")
    class RefreshImages {

        @Test
        @DisplayName("solo procesa los autos sin imagen, guarda los que consiguieron foto y devuelve cuántos son")
        void refreshesOnlyCarsWithoutImage() {
            CarEntity found = carWith(1L, "Yaris", null);
            CarEntity notFound = carWith(2L, "Corolla", null);
            CarEntity alreadyHasImage = carWith(3L, "Hilux", "https://old.jpg");
            when(carRepository.findAllByOrderByIdAsc()).thenReturn(List.of(found, notFound, alreadyHasImage));
            when(pexelsClient.searchCarPhoto(eq("Toyota"), eq("Yaris"), any(), any())).thenReturn(Optional.of(photo()));

            int updated = service.refreshMissingImages();

            assertThat(updated).isEqualTo(1);
            assertThat(found.getImageUrl()).isEqualTo("https://img.pexels.com/landscape.jpg");
            verify(carRepository).save(found);
            verify(carRepository, never()).save(notFound);
            verify(carRepository, never()).save(alreadyHasImage);
            verify(pexelsClient, never()).searchCarPhoto(eq("Toyota"), eq("Hilux"), any(), any());
        }

        @Test
        @DisplayName("si todos los autos ya tienen imagen, no consulta Pexels y devuelve 0")
        void nothingToDo() {
            when(carRepository.findAllByOrderByIdAsc()).thenReturn(List.of(carWith(1L, "Yaris", "https://old.jpg")));

            assertThat(service.refreshMissingImages()).isZero();

            verifyNoInteractions(pexelsClient);
            verify(carRepository, never()).save(any());
        }
    }

    // =====================================================================================
    // Utilidades
    // =====================================================================================

    private void givenCar(CarEntity car) {
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
    }

    private void givenAnyCar(CarEntity car) {
        when(carRepository.findAnyById(CAR_ID)).thenReturn(Optional.of(car));
    }

    private CarRequestDTO carRequest() {
        CarRequestDTO dto = new CarRequestDTO();
        dto.setLicensePlate("ABCD12");
        dto.setBrand("Toyota");
        dto.setModel("Yaris");
        dto.setYear(2023);
        dto.setColor("Blanco");
        dto.setCategory(EnumCategory.SEDAN);
        dto.setFuel(EnumFuel.GASOLINA);
        dto.setSeats(5);
        dto.setMileage(10_000);
        dto.setDailyRate(30_000L);
        return dto;
    }

    // Auto operativo de Toyota con 10.000 km y tarifa de 30.000
    private CarEntity carWith(long id, String model, String imageUrl) {
        CarEntity car = new CarEntity();
        ReflectionTestUtils.setField(car, "id", id);
        car.setLicensePlate("ABCD12");
        car.setBrand("Toyota");
        car.setModel(model);
        car.setYear(2023);
        car.setColor("Blanco");
        car.setCategory(EnumCategory.SEDAN);
        car.setFuel(EnumFuel.GASOLINA);
        car.setSeats(5);
        car.setMileage(10_000);
        car.setAvailability(true);
        car.setDailyRate(30_000L);
        car.setImageUrl(imageUrl);
        car.setDeleted(false);
        return car;
    }

    private PexelsPhoto photo() {
        return new PexelsPhoto(
                "https://www.pexels.com/photo/1",
                "Ana",
                "https://www.pexels.com/@ana",
                new PexelsSrc("https://img.pexels.com/landscape.jpg", null, null));
    }
}