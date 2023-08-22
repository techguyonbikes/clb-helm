package com.tvf.clb.base.model.colossalbet;

import com.tvf.clb.base.utils.AppConstant;

public enum ColBetRaceTypeEnum {
    HORS("HORS", AppConstant.HORSE_RACING),
    HARN("HARN", AppConstant.HARNESS_RACING),
    GREY("GREY", AppConstant.GREYHOUND_RACING);

    private final String rawValue;
    private final String actualValue;

    ColBetRaceTypeEnum(String rawValue, String actualValue) {
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
        for (ColBetRaceTypeEnum typeEnum: ColBetRaceTypeEnum.values()) {
            if (typeEnum.getRawValue().equals(raw)) {
                return typeEnum.getActualValue();
            }
        }
        return null;
    }

}
