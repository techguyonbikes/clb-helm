package com.tvf.clb.base.dto;
import com.tvf.clb.base.utils.AppConstant;
import lombok.Getter;

@Getter
public enum RaceTypeEnum {
    HORSE_RACING(1, AppConstant.HORSE_RACING),
    GREYHOUND_RACING(2, AppConstant.GREYHOUND_RACING),
    HARNESS_RACING(3, AppConstant.HARNESS_RACING);
    private final int id;
    private final String name;

    RaceTypeEnum(int id, String name) {
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
