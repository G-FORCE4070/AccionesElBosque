package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlpacaClockDto(@JsonProperty("is_open") boolean open) {}
