package com.tvf.clb.base.dto;

import com.tvf.clb.base.utils.AppConstant;
import lombok.Getter;

@Getter
public enum SiteEnum {

    LAD_BROKE(1, AppConstant.LAD_BROKE, AppConstant.LAD_BROKE_STATUS_PRIORITY, AppConstant.LAD_BROKE_POSITION_PRIORITY),
    NED(2, AppConstant.NED, AppConstant.NED_STATUS_PRIORITY, AppConstant.NED_POSITION_PRIORITY),
    ZBET(3, AppConstant.ZBET, AppConstant.ZBET_STATUS_PRIORITY, AppConstant.ZBET_POSITION_PRIORITY),
    POINT_BET(4, AppConstant.POINT_BET, AppConstant.POINT_BET_STATUS_PRIORITY, AppConstant.POINT_BET_POSITION_PRIORITY),
    TAB(5, AppConstant.TAB, AppConstant.TAB_STATUS_PRIORITY, AppConstant.TAB_POSITION_PRIORITY),
    SPORT_BET(6, AppConstant.SPORT_BET, AppConstant.SPORT_BET_STATUS_PRIORITY, AppConstant.SPORT_BET_POSITION_PRIORITY),
    TOP_SPORT(7, AppConstant.TOP_SPORT, AppConstant.TOP_SPORT_STATUS_PRIORITY, AppConstant.TOP_SPORT_POSITION_PRIORITY),
    BET_FLUX(9, AppConstant.BET_FLUX, AppConstant.BET_FLUX_STATUS_PRIORITY, AppConstant.BET_FLUX_POSITION_PRIORITY);

    private final int id;
    private final String name;
    private final int statusPriority;
    private final int positionPriority;

    SiteEnum(int id, String name, int statusPriority, int positionPriority) {
        this.id = id;
        this.name = name;
        this.statusPriority = statusPriority;
        this.positionPriority = positionPriority;
    }

    public static String getSiteNameById(int id) {
        for (SiteEnum siteEnum : SiteEnum.values()) {
            if (siteEnum.getId() == id) {
                return siteEnum.getName();
            }
        }
        return null;
    }
}
