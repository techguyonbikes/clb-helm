package com.tvf.clb.base.model.betm;

public enum BetMRaceStatusEnum {
    OPEN("Open"),
    CLOSED("Closed"),
    INTERIM("Interim"),
    FINAL("Final"),
    ABANDONED("Abandoned");

    private final String value;

    BetMRaceStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValueFromRawData(String raw) {
        for (BetMRaceStatusEnum statusEnum: BetMRaceStatusEnum.values()) {
            if (statusEnum.getValue().equals(raw)) {
                return statusEnum.toString();
            }
        }
        return null;
    }
}
