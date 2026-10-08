package co.edu.unbosque.acciones_elbosque.markets_service.dto;

import lombok.Data;

@Data
public class AlpacaSnapshotDto {

    private LatestTrade latestTrade;
    private PrevDailyBar prevDailyBar;

    @Data
    public static class LatestTrade {
        private double p;
    }

    @Data
    public static class PrevDailyBar {
        private double c;
    }
}
