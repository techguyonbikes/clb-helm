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
import java.util.Objects;

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

    private Integer distance;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Race)) return false;

        Race race = (Race) o;

        if (!Objects.equals(raceId, race.raceId)) return false;
        if (!Objects.equals(meetingId, race.meetingId)) return false;
        if (!Objects.equals(name, race.name)) return false;
        if (!Objects.equals(number, race.number)) return false;
        if (!Objects.equals(advertisedStart, race.advertisedStart))
            return false;
        if (!Objects.equals(actualStart, race.actualStart)) return false;
        if (!Objects.equals(mainMarketStatusId, race.mainMarketStatusId))
        if (!Objects.equals(distance, race.distance)) return false;
        return Objects.equals(resultsDisplay, race.resultsDisplay);
    }

    @Override
    public int hashCode() {
        int result = raceId != null ? raceId.hashCode() : 0;
        result = 31 * result + (meetingId != null ? meetingId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (advertisedStart != null ? advertisedStart.hashCode() : 0);
        result = 31 * result + (actualStart != null ? actualStart.hashCode() : 0);
        result = 31 * result + (mainMarketStatusId != null ? mainMarketStatusId.hashCode() : 0);
        result = 31 * result + (resultsDisplay != null ? resultsDisplay.hashCode() : 0);
        result = 31 * result + (distance != null ? distance.hashCode() : 0);
        return result;
    }
    public enum Status {
        O("OPEN"),
        F("FINAL"),
        C("CLOSE");

        public final String label;

        Status(String label) {
            this.label = label;
        }
    }


}
