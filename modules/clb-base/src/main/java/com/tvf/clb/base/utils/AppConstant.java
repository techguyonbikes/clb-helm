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
    public static final String SPORT_BET = "SPORTBET";

    //Because we use ladbroke to store common data , bellow is list site after common data is saved
//    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED, TAB, POINT_BET));

    public static final Integer LAD_BROKE_SITE_ID = 1;
    public static final Integer NED_SITE_ID = 2;
    public static final Integer POINT_BET_SITE_ID = 4;
    public static final Integer ZBET_SITE_ID = 3;
    public static final Integer TAB_SITE_ID = 5;
    public static final Integer SPORTBET_SITE_ID = 6;
    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED, ZBET, TAB, POINT_BET, SPORT_BET));

    public static final String POSITION = "position";

    public static final String ADDITIONAL_INFO = "additional_info";

    public static final String DISTANCE = "distance";

    public static final String ADVERTISED_START = "advertised_start";
    public static final String SECONDS = "seconds";

    public static final String MARKETS_NAME = "Final Field";

    public static final String DEFAULT_LOG_FILE = "logs/cloudbet.log";
    public static final String DEFAULT_LOG_PATH = "logs/";
    public static final String NOT_FOUND = "Not Found";

    public static final String LINE_BREAK_HTML = "<br>";

    public static final String SCRATCHED_NAME = "LateScratched";
    public static final String NOT_SCRATCHED_NAME = "not scratched";

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
    public static final String TAB_BET_MEETING_QUERY = "https://api.beta.tab.com.au/v1/tab-info-service/racing/dates/{date}/meetings?jurisdiction=NSW&returnOffers=true&returnPromo=false";
    public static final String TAB_BET_RACE_QUERY= "https://api.beta.tab.com.au/v1/tab-info-service/racing/dates/{id}?returnPromo=true&returnOffers=true&jurisdiction=NSW";
    public static final String SPORT_BET_MEETING_QUERY= "https://www.sportsbet.com.au/apigw/sportsbook-racing/Sportsbook/Racing/AllRacing/{date}";
    public static final String SPORT_BET_RACE_QUERY="https://www.sportsbet.com.au/apigw/sportsbook-racing/Sportsbook/Racing/Events/{id}/RacecardWithContext";

    // Thêm NZ nếu có new zealand
    public static final List<String> VALID_COUNTRY_CODE = new ArrayList<>(Arrays.asList("AU","AUS","NZL","NZ"));

    public static final List<String> HARNESS_TYPE_RACE = new ArrayList<>(Arrays.asList("harness_racing", "H"));
    public static final List<String> HORSE_TYPE_RACE = new ArrayList<>(Arrays.asList("horse_racing","R"));
    public static final List<String> GREYHOUND_TYPE_RACE = new ArrayList<>(Arrays.asList("greyhound_racing","G"));
    public static final List<String> VALID_LOCATION_CODE = new ArrayList<>(Arrays.asList("NSW", "SA", "VIC", "QLD", "NT", "TAS", "WA","NZL","ACT"));
    public static final List<String> VALID_CHECK_CODE_STATE_DIFF = Arrays.asList("NSW", "SA", "VIC", "QLD", "NT", "TAS", "WA", "NZ", "NZL","ACT");

    public static final String VALID_CHECK_PRODUCT_CODE = "ZBF";

    public static final String CODE_NZ =  "NZ";
    public static final String CODE_NZL =  "NZL";
    //racetpye by sportBet
    public static final String HARNESS_RACE_TYPE = "harness";
    public static final String HORSE_RACE_TYPE = "horse";
    public static final String GREYHOUND_RACE_TYPE = "greyhound";
    public static final List<String> VALID_COUNTRY_SPORT_BET = new ArrayList<>(Arrays.asList("Australia","New Zealand"));
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_INTERIM = "INTERIM";
    public static final String STATUS_FINAL = "FINAL";
    public static final String STATUS_ABANDONED = "ABANDONED";
    public static final String PRICE_CODE = "L";
    public static final String RACECARD_EVENT="racecardEvent";
    public static final String ZBET_SELLING_STATUS = "selling";
    public static final String ZBET_PAID_STATUS = "paid";
    public static final String ZBET_PAYING_STATUS = "paying";
    public static final String ZBET_ABANDONED_STATUS = "abandoned";
    public static final String ZBET_INTERIM_STATUS = "interim";
    public static final String ZBET_CLOSED_STATUS = "closed";
    public static final int DATE_REMOVE_DATA = 7;

    // Time
    public static final int HOUR_TIME = 23;
    public static final int MINUTE_TIME = 59;
    public static final int SECOND_TIME = 59;
    public static final int ZERO_TIME = 0;

    public static final String SPORT_BET_BETTING_STATUS_RESULTED = "RESULTED";

    public static final String SPORT_BET_BETTING_STATUS_OFF = "OFF";

    public static final String TAB_RACE_STATUS_FINAL = "Paying";

    public static final Integer MAX_RETRIES = 2;

    public static final long RETRY_DELAY_TIME = 1000L;

}
