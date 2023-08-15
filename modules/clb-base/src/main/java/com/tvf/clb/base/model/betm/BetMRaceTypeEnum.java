package com.tvf.clb.base.model.betm;

import com.tvf.clb.base.utils.AppConstant;

public enum BetMRaceTypeEnum {
    HORSE("T", AppConstant.HORSE_RACING),
    HARNESS("H", AppConstant.HARNESS_RACING),
    GREYHOUND("G", AppConstant.GREYHOUND_RACING);

    private final String rawValue;
    private final String actualValue;

    BetMRaceTypeEnum(String rawValue, String actualValue) {
        this.rawValue = rawValue;
        this.actualValue = actualValue;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public static String getValueFromRawData(String raw) {
        for (BetMRaceTypeEnum typeEnum: BetMRaceTypeEnum.values()) {
            if (typeEnum.getRawValue().equals(raw)) {
                return typeEnum.getActualValue();
            }
        }
        return null;
    }

}
