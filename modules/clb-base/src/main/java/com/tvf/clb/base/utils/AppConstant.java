package com.tvf.clb.base.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppConstant {
    public static final String LAD_BROKE = "LADBROKE";
    public static final String NED = "NED";

    //Because we use ladbroke to store common data , bellow is list site after common data is saved
    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED));

    public static final String POSITION = "position";

    public static final String AUS = "AUS";
    public static final String DATE_PARAM = "{date}";

    public static final String ID_PARAM = "{id}";
    public static final String LAD_BROKES_IT_MEETING_QUERY = "https://api.ladbrokes.com.au/v2/racing/meeting?date={date}&region=domestic&timezone=Asia%2FBangkok";

    public static final String LAD_BROKES_IT_RACE_QUERY= "https://api.ladbrokes.com.au/rest/v1/racing/?method=racecard&id={id}";

    public static final String NEDS_MEETING_QUERY = "https://api.neds.com.au/v2/racing/meeting?date={date}&region=domestic&timezone=Asia%2FBangkok";

    public static final String NEDS_RACE_QUERY= "https://api.neds.com.au/rest/v1/racing/?method=racecard&id={id}";
    public static final String HARNESS_RACING = "Harness Racing";
    public static final String GREYHOUND_RACING = "Greyhound Racing";
    public static final String HORSE_RACING = "Horse Racing";

    public static final String HARNESS_FEED_TYPE = "harness_racing";
    public static final String HORSE_FEED_TYPE = "horse_racing";
    public static final String GREYHOUND_FEED_TYPE = "greyhound_racing";
    public static final String DATE_TIME_FORMAT_LONG = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final long TIME_VALIDATE_START = 6000;
}
