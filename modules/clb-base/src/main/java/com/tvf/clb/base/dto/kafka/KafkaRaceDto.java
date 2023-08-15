package com.tvf.clb.base.dto.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class KafkaRaceDto {
    private Long id;
    private Long meetingId;
    private String venueId;
    private String name;
    private Integer number;
    private String status;
    private String raceType;
    private String advertisedStart;
    private String actualStart;
    private Integer distance;
    private String silkUrl;
    private String fullFormUrl;
    private Map<String, String> mapSiteUUID;
    private Map<String, String> finalResult;
    private Map<String, String> interimResult;
    private Map<String, String> raceSiteUrl;
    private List<KafkaEntrantDto> entrants;
}
