package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.exception.AlpacaAccountCreationException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AlpacaBrokerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AlpacaBrokerServiceImpl implements AlpacaBrokerService {

    private final RestClient alpacaRestClient;

    @Value("${alpaca.broker.firm-account-id}")
    private String firmAccountId;

    public AlpacaBrokerServiceImpl(@Qualifier("alpacaRestClient") RestClient alpacaRestClient) {
        this.alpacaRestClient = alpacaRestClient;
    }

    @PostConstruct
    public void validateConfig() {
        if ("test-firm-id".equals(firmAccountId)) {
            log.warn("ALPACA_FIRM_ACCOUNT_ID is a placeholder — set it to your broker sweep account ID. Funding will fail.");
        } else {
            log.info("Alpaca firm account configured: {}", firmAccountId);
        }
    }

    @Override
    public String createAccount(User user) {
        Map<String, Object> payload = buildAccountPayload(user);
        log.debug("Creating Alpaca account for: {}", user.getEmail());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = alpacaRestClient.post()
                    .uri("/v1/accounts")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            String alpacaId = (String) response.get("id");
            log.info("Alpaca account created successfully: alpacaId={}", alpacaId);
            return alpacaId;
        } catch (HttpClientErrorException ex) {
            log.error("Alpaca account creation rejected [HTTP {}]: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AlpacaAccountCreationException("Alpaca rejected account creation: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Alpaca account creation failed: {}", ex.getMessage());
            throw new AlpacaAccountCreationException("Alpaca account creation failed");
        }
    }

    @Override
    public void fundAccount(String alpacaAccountId) {
        log.debug("Funding Alpaca account via ACH: {}", alpacaAccountId);
        activateSandboxAccount(alpacaAccountId);
        String achId = createAchRelationship(alpacaAccountId);
        // Give Alpaca a short window to begin processing the relationship,
        // then submit the transfer. Sandbox queues both and processes together.
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        createAchTransfer(alpacaAccountId, achId);
        log.info("Alpaca account ACH funding queued: alpacaId={} — funds available once sandbox processes the transfer", alpacaAccountId);
    }

    private void activateSandboxAccount(String alpacaAccountId) {
        try {
            alpacaRestClient.post()
                    .uri("/v1/sandbox/accounts/{id}/action", alpacaAccountId)
                    .body(Map.of("action", "APPROVE"))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sandbox account {} approved", alpacaAccountId);
        } catch (Exception ex) {
            log.warn("Could not approve sandbox account {} (may already be active): {}", alpacaAccountId, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String createAchRelationship(String alpacaAccountId) {
        Map<String, Object> payload = Map.of(
                "account_owner_name", "AEB Trading User",
                "bank_account_type", "CHECKING",
                "bank_account_number", "32131231abc",
                "bank_routing_number", "123103716",
                "nickname", "Sandbox Bank"
        );
        try {
            Map<String, Object> resp = alpacaRestClient.post()
                    .uri("/v1/accounts/{id}/ach_relationships", alpacaAccountId)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            String achId = resp != null ? (String) resp.get("id") : null;
            log.info("ACH relationship created: achId={} for alpacaId={}", achId, alpacaAccountId);
            return achId;
        } catch (Exception ex) {
            log.error("ACH relationship creation failed for {}: {}", alpacaAccountId, ex.getMessage());
            throw new AlpacaAccountCreationException("ACH relationship creation failed");
        }
    }


private void createAchTransfer(String alpacaAccountId, String achId) {
        Map<String, Object> payload = Map.of(
                "transfer_type", "ach",
                "direction", "INCOMING",
                "timing", "immediate",
                "amount", "50000",
                "relationship_id", achId
        );
        try {
            alpacaRestClient.post()
                    .uri("/v1/accounts/{id}/transfers", alpacaAccountId)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("ACH transfer of $50,000 created for alpacaId={}", alpacaAccountId);
        } catch (HttpClientErrorException ex) {
            log.error("ACH transfer rejected [HTTP {}]: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AlpacaAccountCreationException("ACH transfer rejected: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("ACH transfer failed: {}", ex.getMessage());
            throw new AlpacaAccountCreationException("ACH transfer failed");
        }
    }

    private String mapFundingSource(String incomeSource) {
        return switch (incomeSource) {
            case "business"    -> "business_income";
            case "investments" -> "investments";
            case "pension"     -> "savings";
            default            -> "employment_income";
        };
    }

    private String mapTaxIdType(String docType) {
        return switch (docType) {
            case "cc" -> "COL_CC";
            // COL_CC is the closest match Alpaca supports for Colombian residents
            default   -> "COL_CC";
        };
    }

    private String mapCountryAlpha3(String alpha2) {
        return switch (alpha2) {
            case "CO" -> "COL";
            case "US" -> "USA";
            case "MX" -> "MEX";
            case "AR" -> "ARG";
            case "BR" -> "BRA";
            case "CL" -> "CHL";
            case "PE" -> "PER";
            case "VE" -> "VEN";
            case "EC" -> "ECU";
            case "BO" -> "BOL";
            case "PY" -> "PRY";
            case "UY" -> "URY";
            case "PA" -> "PAN";
            case "CR" -> "CRI";
            case "GT" -> "GTM";
            case "DO" -> "DOM";
            case "ES" -> "ESP";
            case "GB" -> "GBR";
            case "DE" -> "DEU";
            case "FR" -> "FRA";
            case "CA" -> "CAN";
            case "AU" -> "AUS";
            default   -> alpha2.length() == 3 ? alpha2 : "COL";
        };
    }

    /** Strips diacritics and removes any remaining non-ASCII characters (e.g. "Bogotá" → "Bogota"). */
    private String toAscii(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        return nfd.replaceAll("[^\\x00-\\x7F]", "").trim();
    }

    private Map<String, Object> buildAccountPayload(User user) {
        String now = Instant.now().toString();
        String countryAlpha3 = mapCountryAlpha3(user.getCountry());
        return Map.of(
                "contact", Map.of(
                        "email_address", user.getEmail(),
                        "phone_number", user.getPhone(),
                        "street_address", List.of(toAscii(user.getAddress())),
                        "city", toAscii(user.getCity()),
                        "postal_code", user.getPostalCode(),
                        "country", countryAlpha3
                ),
                "identity", Map.of(
                        "given_name", toAscii(user.getFirstName()),
                        "family_name", toAscii(user.getLastName()),
                        "date_of_birth", user.getBirthDate().toString(),
                        "country_of_tax_residence", countryAlpha3,
                        "funding_source", List.of(mapFundingSource(user.getIncomeSource())),
                        "tax_id", user.getDocNumber(),
                        "tax_id_type", mapTaxIdType(user.getDocType())
                ),
                "disclosures", Map.of(
                        "is_control_person", false,
                        "is_affiliated_exchange_or_finra", user.isFinraAffiliated(),
                        "is_politically_exposed", user.isPep(),
                        "immediate_family_exposed", false
                ),
                "agreements", List.of(
                        Map.of("agreement", "account_agreement",   "signed_at", now, "ip_address", "127.0.0.1"),
                        Map.of("agreement", "customer_agreement",  "signed_at", now, "ip_address", "127.0.0.1")
                )
        );
    }
}
