package co.edu.unbosque.acciones_elbosque.identity_service.model;

import java.io.Serializable;

public class MfaEntry implements Serializable {

    private String code;
    private int attempts;
    private int resends;

    public MfaEntry(String code, int attempts, int resends) {
        this.code = code;
        this.attempts = attempts;
        this.resends = resends;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public int getResends() { return resends; }
    public void setResends(int resends) { this.resends = resends; }
}
