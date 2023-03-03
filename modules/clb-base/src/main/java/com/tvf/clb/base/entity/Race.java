package com.tvf.clb.base.entity;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tvf.clb.base.utils.PgJsonObjectDeserializer;
import com.tvf.clb.base.utils.PgJsonObjectSerializer;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("race")
public class Race {
    private Long id;
    private String raceId;
    private String meetingId;
    private String name;
    private Integer number;
    private Instant advertisedStart;
    private Instant actualStart;
    @JsonSerialize(using = PgJsonObjectSerializer.class)
    @JsonDeserialize(using = PgJsonObjectDeserializer.class)
    private Json marketIds;
    private String mainMarketStatusId;
    private String resultsDisplay;

//    public List<String> getMarketIds() {
//        Gson gson = new Gson();
//        Type type =new TypeToken<List<String>>() {}.getType();
//        return gson.fromJson(marketIds, type);
//    }
}
