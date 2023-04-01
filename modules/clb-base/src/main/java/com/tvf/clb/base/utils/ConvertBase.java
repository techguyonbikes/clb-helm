package com.tvf.clb.base.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConvertBase {
    public static String convertRaceTypeOfTab(String feedId) {
        String type = null;
        switch (feedId) {
            case "G":
                type = AppConstant.GREYHOUND_RACING;
                break;
            case "R":
                type = AppConstant.HORSE_RACING;
                break;
            case "H":
                type = AppConstant.HARNESS_RACING;
                break;

        }
        return type;
    }

}