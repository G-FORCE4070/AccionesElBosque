package co.edu.unbosque.acciones_elbosque.trading_service.exception;

public class PriceServiceUnavailableException extends RuntimeException {
    public PriceServiceUnavailableException() {
        super("Servicio de precios no disponible temporalmente");
    }
}
