package com.tvf.clb.base.dto;

import lombok.Getter;

@Getter
public enum SiteEnum {

    LAD_BROKE(1, "LADBROKE"),
    NED(2, "NED"),
    ZBET(3, "ZBET"),
    POINT_BET(4, "POINTBET"),
    TAB(5, "TAB"),
    SPORT_BET(6, "SPORTBET"),
    TOP_SPORT(7, "TOPSPORT");

    private final int id;
    private final String name;

    SiteEnum(int id, String name) {
        this.id = id;
        this.name = name;
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
