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
    public static final String TOP_SPORT = "TOPSPORT";
    public static final Integer LAD_BROKE_SITE_ID = 1;
    public static final Integer NED_SITE_ID = 2;
    public static final Integer POINT_BET_SITE_ID = 4;
    public static final Integer ZBET_SITE_ID = 3;
    public static final Integer TAB_SITE_ID = 5;
    public static final Integer SPORTBET_SITE_ID = 6;
    public static final Integer TOPSPORT_SITE_ID = 7;
    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED, ZBET, TAB, POINT_BET, SPORT_BET, TOP_SPORT));
    public static final String POSITION = "position";

    public static final String DISTANCE = "distance";
    public static final String MARKETS_NAME = "Final Field";

    public static final String SCRATCHED_NAME = "LateScratched";
    public static final String NOT_SCRATCHED_NAME = "not scratched";
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
    //Topsport
    public static final String TOPSPORT_MEETING_QUERY= "https://www.topsport.com.au/Racing/All/{date}";
    public static final String TOPSPORT_RACE_QUERY= "https://www.topsport.com.au/{id}";
    public static final String TODAY = "Today";
    public static final String YESTERDAY = "Yesterday";
    public static final String TOMORROW = "Tomorrow";

    public static final String SPORT_BET_BETTING_STATUS_RESULTED = "RESULTED";

    public static final String SPORT_BET_BETTING_STATUS_OFF = "OFF";

    public static final String TAB_RACE_STATUS_FINAL = "Paying";

    public static final Integer MAX_RETRIES = 2;

    public static final long RETRY_DELAY_TIME = 1000L;

    public static final String REPLACE_STRING = "[^\\w\\s]";
    public static final String CLASS = "class";
    public static final String ROW = "tr";
    public static final String COLUMN = "th";
    public static final String CELL = "td";
    public static final String SPAN = "span";
    public static final String LINK = "a";
    public static final String HREF = "href";
    public static final String SCRATCHED_REPLACE = "Scratched @ ";
    public static final String BODY = "tbody";
    public static final String RACE_NAME = "raceName";
    public static final String RACE_INFORMATION = "raceInformation";
    public static final String RACE_NUMBER = "div[class=raceNum]";
    public static final String RACE_RESULT = "div[class=result]";
    public static final String SECTION_CLASS = "section[class=framePanel race]";
    public static final String NAME_CLASS = "name";
    public static final String SADDLE_CLASS = "saddle";
    public static final String BARRIER_CLASS = "barrier";
    //CSS QUERY
    public static final String SCRATCHED_QUERY = "tr[class=scratched]";
    public static final String FIXED_WIN_QUERY = "a[data-special=FWIN]";
    public static final String SILKCOLUMN_QUERY = "tr:has(td[class=silkColumn])";
    public static final String FIXED_PLACE_QUERY = "a[data-special=FPLC]";
    public static final String DATA_PRICE = "data-price";
    public static final String SCRATCHPAY_CLASS = "scratchPay";
    public static final String RACEREGION = "raceRegion";
    public static final String RACESTATE = "raceState";
}
