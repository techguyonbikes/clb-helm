package com.tvf.clb.base.model.zbet;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public class ZBetRacesData {
    private Long id;
    private Long number;
    @SerializedName(" race_site_link")
    private String raceUrl;
    private Instant date;
    private String type;

}