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
    public static final String PLAY_UP = "PLAY_UP";
    public static final String TOP_SPORT = "TOPSPORT";
    public static final String BET_FLUX = "BETFLUX";
    public static final String BET_M = "BETM";
    public static final String COLOSSAL_BET = "COLOSSAL_BET";
    public static final String BLUE_BET = "BLUEBET";
    public static final String BET_RIGHT = "BETRIGHT";
    public static final Integer LAD_BROKE_SITE_ID = 1;
    public static final Integer NED_SITE_ID = 2;
    public static final Integer POINT_BET_SITE_ID = 4;
    public static final Integer TAB_SITE_ID = 5;
    public static final Integer SPORTBET_SITE_ID = 6;
    public static final Integer TOPSPORT_SITE_ID = 7;
    public static final Integer BET_M_SITE_ID = 8;
    public static final Integer COLOSSAL_BET_SITE_ID = 11;
    public static final Integer BLUE_BET_SITE_ID = 12;
    public static final List<String> SITE_LIST = new ArrayList<>(Arrays.asList(NED, ZBET, TAB, POINT_BET, SPORT_BET, TOP_SPORT, BET_M, BET_FLUX, PLAY_UP, BLUE_BET, COLOSSAL_BET, BET_RIGHT));
    public static final String RACE_NAME_DEFAULT = "RACE";
    public static final Integer PLAY_UP_SITE_ID = 10;
    public static final String POSITION = "position";

    public static final String DISTANCE = "distance";
    public static final String MARKETS_NAME = "Final Field";

    public static final String SCRATCHED_NAME = "LateScratched";
    public static final String NOT_SCRATCHED_NAME = "not scratched";
    public static final String DATE_PARAM = "{date}";
    public static final String ID_PARAM = "{id}";

    public static final String LAD_BROKES_BASE_URL = "https://api.ladbrokes.com.au";
    public static final String LAD_BROKES_IT_MEETING_QUERY = "/v2/racing/meeting?date={date}&timezone=Kingston/Adelaide/Perth";

    public static final String LAD_BROKES_IT_RACE_QUERY= "/rest/v1/racing/?method=racecard&id={id}";

    public static final String NEDS_BASE_URL = "https://api.neds.com.au";

    public static final String NEDS_MEETING_QUERY = "/v2/racing/meeting?date={date}&timezone=Kingston/Adelaide/Perth";

    public static final String NEDS_RACE_QUERY= "/rest/v1/racing/?method=racecard&id={id}";

    public static final String POINT_BET_BASE_URL = "https://api.au.pointsbet.com/api/v2/racing";

    public static final String POINT_BET_MEETING_QUERY = "/meetings/index?localdate={date}&localstartoffsetmins=420&localendoffsetmins=420";

    public static final String POINT_BET_RACE_QUERY= "/races/{id}";

    public static final String ZBET_BASE_URL = "https://api.zbet.com.au";

    public static final String ZBET_MEETING_QUERY = "/api/v2/combined/meetings/races?date={date}";

    public static final String ZBET_RACE_QUERY = "/api/v2/combined/race/selections?race_id={id}";

    public static final String TAB_BASE_URL = "https://api.beta.tab.com.au";

    public static final String TAB_MEETING_QUERY = "/v1/tab-info-service/racing/dates/{date}/meetings?jurisdiction=NSW&returnOffers=true&returnPromo=false";

    public static final String TAB_RACE_QUERY= "/v1/tab-info-service/racing/dates/{id}?returnPromo=true&returnOffers=true&jurisdiction=NSW";

    public static final String SPORT_BET_BASE_URL = "https://www.sportsbet.com.au/apigw/sportsbook-racing/Sportsbook/Racing";

    public static final String SPORT_BET_MEETING_QUERY= "/AllRacing/{date}";

    public static final String SPORT_BET_RACE_QUERY="/Events/{id}/RacecardWithContext";
    //betFlux
    public static final String BET_FLUX_BASE_URL = "https://api.betflux.com.au";

    public static final String BET_FLUX_MEETING_QUERY = "/api/v2/combined/meetings/races?date={date}";

    public static final String BET_FLUX_RACE_QUERY = "/api/v2/combined/race/selections?race_id={id}";

    public static final String BET_M_BASE_URL = "https://api.betm.com.au/v1/fixtures/races";

    public static final String BET_M_MEETING_QUERY = "/{date}/A";

    public static final String BET_M_RACE_QUERY = "/{id}";

    //BetRight
    public static final String BET_RIGHT_BASE_URL="https://next-api.betright.com.au";
    public static final String BET_RIGHT_MEETING_QUERY="/Racing/GroupedRaceCard?raceDate={date}";
    public static final String BET_RIGHT_RACE_QUERY="/Racing/Event?eventId={id}";
    public static final String BET_RIGHT_RACE_URL="https://www.betright.com.au/racing/{id}";
    public static final Integer BET_RIGHT_FINAL_RESULTS = 2;

    public static final String BLUE_BET_BASE_URL = "https://web20-api.bluebet.com.au";

    public static final String BLUE_BET_MEETING_QUERY = "/GroupedRaceCard/?date={date}&format=json";

    public static final String BLUE_BET_RACE_QUERY = "/race?eventId={id}&format=json";

    public static final String HARNESS_RACING = "Harness Racing";
    public static final String GREYHOUND_RACING = "Greyhound Racing";
    public static final String HORSE_RACING = "Horse Racing";

    public static final String HARNESS_FEED_TYPE = "harness_racing";
    public static final String HORSE_FEED_TYPE = "horse_racing";
    public static final String GREYHOUND_FEED_TYPE = "greyhound_racing";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    public static final ZoneId AU_ZONE_ID = ZoneId.of("Australia/Sydney");

    public static final String AUS = "AUS";

    public static final List<String> HARNESS_TYPE_RACE = new ArrayList<>(Arrays.asList("harness_racing", "H"));
    public static final List<String> HORSE_TYPE_RACE = new ArrayList<>(Arrays.asList("horse_racing","R"));
    public static final List<String> GREYHOUND_TYPE_RACE = new ArrayList<>(Arrays.asList("greyhound_racing","G"));

    public static final String VALID_CHECK_PRODUCT_CODE = "ZBF";
    public static final String VALID_CHECK_PRODUCT_CODE_BET_FLUX = "TBF";

    public static final String CODE_NZ =  "NZ";
    public static final String CODE_NZL =  "NZL";
    //racetpye by sportBet
    public static final String HARNESS_RACE_TYPE = "harness";
    public static final String HORSE_RACE_TYPE = "horse";
    public static final String GREYHOUND_RACE_TYPE = "greyhound";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_INTERIM = "INTERIM";
    public static final String STATUS_FINAL = "FINAL";
    public static final String STATUS_ABANDONED = "ABANDONED";
    public static final String STATUS_RE_RESULTED = "RE-RESULTED";
    public static final String PRICE_CODE = "L";
    public static final String ZBET_SELLING_STATUS = "selling";
    public static final String ZBET_PAID_STATUS = "paid";
    public static final String ZBET_PAYING_STATUS = "paying";
    public static final String ZBET_ABANDONED_STATUS = "abandoned";
    public static final String ZBET_INTERIM_STATUS = "interim";
    public static final String ZBET_CLOSED_STATUS = "closed";
    public static final int DATE_REMOVE_DATA = 7;

    public static final int RACE_SIZE = 7;

    // Time
    public static final int HOUR_TIME = 23;
    public static final int MINUTE_TIME = 59;
    public static final int SECOND_TIME = 59;
    public static final int ZERO_TIME = 0;
    //Topsport
    public static final String TAG_DISTANCE = "DISTANCE";
    public static final String TAG_START_TIME = "STARTS_TIME";
    public static final String TOPSPORT_MEETING_QUERY= "/Racing/All/{date}";
    public static final String TOPSPORT_RACE_QUERY= "{id}";
    public static final String TOPSPORT_BASE_URL= "https://www.topsport.com.au";
    public static final String TODAY = "Today";
    public static final String YESTERDAY = "Yesterday";
    public static final String TOMORROW = "Tomorrow";

    public static final String SPORT_BET_BETTING_STATUS_RESULTED = "RESULTED";
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
    public static final String OPEN_PRICE_WIN = "span[data-mtype=R]";
    public static final String FLUC_INIT_PRICE = "span[class=fluc flucInit]";
    public static final String SCRATCH_PRICE_QUERY = "td[class=oddsColumn betlink scratchPay]";
    //URL RACE ALL SITE
    public static final String URL_LAD_BROKES_IT_RACE = "https://www.ladbrokes.com.au/racing/{id}";
    public static final String URL_NEDS_RACE = "https://www.neds.com.au/racing/{id}";
    public static final String URL_TAP_RACE = "https://www.tab.com.au/racing/{id}";
    public static final String URL_POINT_BET_RACE = "https://pointsbet.com.au/racing/{id}";
    public static final String URL_SPORT_BET_RACE = "https://www.sportsbet.com.au/{id}";
    public static final String URL_TOPSPORT_RACE = "https://www.topsport.com.au/{id}";
    public static final String URL_BLUEBET_RACE = "https://www.bluebet.com.au/racing/{id}/win";
    public static final String URL_BET_M_RACE = "https://betm.com.au/racing/{id}";
    public static final String URL_COLOSSAL_BET_RACE = "https://www.colossalbet.com.au/race-detail/{id}";

    //Status - Position priority
    public static final int LAD_BROKE_STATUS_PRIORITY = 1;
    public static final int LAD_BROKE_POSITION_PRIORITY = 1;
    public static final int NED_STATUS_PRIORITY = 4;
    public static final int NED_POSITION_PRIORITY = 4;
    public static final int ZBET_STATUS_PRIORITY = 3;
    public static final int ZBET_POSITION_PRIORITY = 3;
    public static final int POINT_BET_STATUS_PRIORITY = 2;
    public static final int POINT_BET_POSITION_PRIORITY = 2;
    public static final int TAB_STATUS_PRIORITY = 5;
    public static final int TAB_POSITION_PRIORITY = 5;
    public static final int SPORT_BET_STATUS_PRIORITY = 6;
    public static final int SPORT_BET_POSITION_PRIORITY = 6;
    public static final int TOP_SPORT_STATUS_PRIORITY = 7;
    public static final int TOP_SPORT_POSITION_PRIORITY = 7;
    public static final int BET_RIGHT_STATUS_PRIORITY = 12;
    public static final int BET_RIGHT_POSITION_PRIORITY = 12;
    public static final int BET_M_STATUS_PRIORITY = 8;
    public static final int BET_M_POSITION_PRIORITY = 8;
    public static final int BET_FLUX_POSITION_PRIORITY = 9;
    public static final int BET_FLUX_STATUS_PRIORITY = 9;
    public static final int BLUE_BET_STATUS_PRIORITY = 12;
    public static final int BLUE_BET_POSITION_PRIORITY = 12;
    public static final int PLAY_UP_STATUS_PRIORITY = 10;
    public static final int PLAY_UP_POSITION_PRIORITY = 10;
    public static final int COLOSSAL_BET_STATUS_PRIORITY = 11;
    public static final int COLOSSAL_BET_POSITION_PRIORITY = 11;

    // STATUS ORDER

    public static final int OPEN_STATUS_ORDER = 1;
    public static final int CLOSE_STATUS_ORDER = 2;
    public static final int INTERIM_STATUS_ORDER = 3;
    public static final int FINAL_STATUS_ORDER = 4;
    public static final int RE_RESULTED_STATUS_ORDER = 4;
    public static final int SUSPENDED_STATUS_ORDER = 4;
    public static final int ABANDONED_STATUS_ORDER = 4;

    // Ladbrokes status

    public static final String LADBROKE_STATUS_OPEN = "4bc8fe96-296b-4b4c-aea4-85c94f63b9c6";
    public static final String LADBROKE_STATUS_LIVE = "0b9e24e1-3daa-4fdd-b4d5-e720cb74a2ce";
    public static final String LADBROKE_STATUS_CLOSED = "6cb39fac-be37-4ef2-8468-b4795aafe7ce";
    public static final String LADBROKE_STATUS_INTERIM = "766bab58-a04c-4cd3-8a3d-fdcfa38bf016";
    public static final String LADBROKE_STATUS_FINAL = "46b6910d-8379-44fb-855c-afbbdee3b007";
    public static final String LADBROKE_STATUS_ABANDONED = "a8419435-bd8f-406d-bdef-734e25a15569";
    public static final String SIDE_NAME_PREFIX = "R";
    public static final String LADBROKE_NEDS_DATA_PRICE_KEY = ":7cf3eea6-5654-42be-9c2e-6de280e7bb34:";
    public static final String PLAY_UP_BASE_URL = "https://wagering-api.playup.io/v1";

    public static final String PLAY_UP_MEETING_QUERY = "/meetings/?include=races&filter[start_date][from]={date}&filter[start_date][to]={date}&filter[is_future]=0&page[size]=1000";

    public static final String PLAY_UP_RACE_QUERY= "/races/{id}/?include=selections.prices,result";
    public static final String URL_PLAY_UP_RACE= "https://www.playup.com.au/betting/racing/{id}";

    public static final String PRICE_SCRATCH_WIN = "SCRATCH WIN";
    public static final String PRICE_SCRATCH_PLACE = "SCRATCH PLACE";
    public static final String PRICE_WIN = "WIN";
    public static final String PRICE_PLACE = "PLACE";
    public static final String PRICE_PLACE_CODE = "PLC";
    public static final String PRICE_REGEX = "\\b(\\d+\\.?\\d*)\\s*([a-zA-Z]+)\\b";
    public static final String CSRF_HEADER_NAME = "X-Csrf-Token";
    public static final String CSRF_TOKEN = "cloud-bet";
    public static final String PLAY_UP_RACE_STATUS_FINAL = "Paid";
    public static final String AUTHORIZATION = "clientKey=colossalbet&timestamp=&signature=";
    public static final String ACCEPT = "application/json, text/plain, */*";
    public static final String COLOSSAL_BET_BASE_MEETING_URL = "https://api.racebookhq.com/api/v1/genweb/events/short";

    public static final String COLOSSAL_BET_MEETING_QUERY = "/{date}";
    public static final String COLOSSAL_BET_BASE_RACE_URL = "https://apicob.generationweb.com.au/GWBetService/r/b/GetEventRace";


    public static final String COLOSSAL_BET_RACE_QUERY = "/{id}";
    public static final String COLOSSAL_BET_RACE_STATUS_FINAL = "Final";

}
