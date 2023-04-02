package com.tvf.clb.base.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class AppConstant {
    public static final String LAD_BROKE = "LADBROKE";
    public static final String NED = "NED";
    public static final String TAB = "TAB";
    public static final String POINT_BET = "POINTBET";
    public static final String ZBET = "ZBET";

    //Because we use ladbroke to store common data , bellow is list site after common data is saved
//    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED, TAB, POINT_BET));

    public static final Integer LAD_BROKE_SITE_ID = 1;
    public static final Integer NED_SITE_ID = 2;
    public static final Integer POINT_BET_SITE_ID = 4;
    public static final Integer ZBET_SITE_ID = 3;
    public static final Integer TAB_SITE_ID = 5;
    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED, ZBET, TAB, POINT_BET));

    public static final String POSITION = "position";

    public static final String ADDITIONAL_INFO = "additional_info";

    public static final String DISTANCE = "distance";

    public static final String MARKETS_NAME = "Final Field";

    public static final String SCRATCHED_NAME = "LateScratched";

    public static final String AUS = "AUS";
    public static final String DATE_PARAM = "{date}";

    public static final String ID_PARAM = "{id}";
    public static final String LAD_BROKES_IT_MEETING_QUERY = "https://api.ladbrokes.com.au/v2/racing/meeting?date={date}&region=domestic&timezone=Asia%2FBangkok";

    public static final String LAD_BROKES_IT_RACE_QUERY= "https://api.ladbrokes.com.au/rest/v1/racing/?method=racecard&id={id}";

    public static final String NEDS_MEETING_QUERY = "https://api.neds.com.au/v2/racing/meeting?date={date}&region=domestic&timezone=Asia%2FBangkok";

    public static final String NEDS_RACE_QUERY= "https://api.neds.com.au/rest/v1/racing/?method=racecard&id={id}";

    public static final String POINT_BET_MEETING_QUERY = "https://api.au.pointsbet.com/api/v2/racing/meetings/index?localdate={date}&localstartoffsetmins=420&localendoffsetmins=420";

    public static final String POINT_BET_RACE_QUERY= "https://api.au.pointsbet.com/api/v2/racing/races/{id}";

    public static final String HARNESS_RACING = "Harness Racing";
    public static final String GREYHOUND_RACING = "Greyhound Racing";
    public static final String HORSE_RACING = "Horse Racing";
    public static final String ZBET_MEETING_QUERY = "https://api.zbet.com.au/api/v2/combined/meetings/races?date={date}";
    public static final String ZBET_RACE_QUERY = "https://api.zbet.com.au/api/v2/combined/race/selections?race_id={id}";

    public static final String HARNESS_FEED_TYPE = "harness_racing";
    public static final String HORSE_FEED_TYPE = "horse_racing";
    public static final String GREYHOUND_FEED_TYPE = "greyhound_racing";
    public static final String DATE_TIME_FORMAT_LONG = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    public static final ZoneId AU_ZONE_ID = ZoneId.of("Australia/Sydney");
    public static final long TIME_VALIDATE_START = 6000;
    public static final String TAB_BET_RACE_QUERY= "https://api.beta.tab.com.au/v1/tab-info-service/racing/dates/{id}";

    // Thêm NZ nếu có new zealand
    public static final List<String> VALID_COUNTRY_CODE = new ArrayList<>(Arrays.asList("AU","AUS","NZL","NZ"));

    public static final List<String> HARNESS_TYPE_RACE = new ArrayList<>(Arrays.asList("harness_racing", "H"));
    public static final List<String> HORSE_TYPE_RACE = new ArrayList<>(Arrays.asList("horse_racing","R"));
    public static final List<String> GREYHOUND_TYPE_RACE = new ArrayList<>(Arrays.asList("greyhound_racing","G"));
}
