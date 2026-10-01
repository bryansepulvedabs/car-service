package rent_a_car_bryan.carservice.exception;

// Operacion invalida sobre un auto (ej. bajar el kilometraje). Sale como 400.
public class InvalidCarOperationException extends RuntimeException {

    public InvalidCarOperationException(String message) {
        super(message);
    }
}