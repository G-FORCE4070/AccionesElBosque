package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;

public interface AlpacaBrokerService {
    String createAccount(User user);
    void fundAccount(String alpacaAccountId);
}
