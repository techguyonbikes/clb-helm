package com.tvf.clb.base.dto;

import com.tvf.clb.base.utils.AppConstant;
import lombok.Getter;

@Getter
public enum RaceTypeBetRightEnum {

    HORSE_RACING(1,AppConstant.HORSE_RACING),
    HARNESS_RACING(2, AppConstant.HARNESS_RACING),
    GREYHOUND_RACING(3, AppConstant.GREYHOUND_RACING);

    private final int id;
    private final String name;

    RaceTypeBetRightEnum(int id, String name) {
        this.id = id;
        this.name = name;

    }

    public static String getSiteNameById(int id) {
        for (RaceTypeEnum siteEnum : RaceTypeEnum.values()) {
            if (siteEnum.getId() == id) {
                return siteEnum.getName();
            }
        }
        return null;
    }
}
