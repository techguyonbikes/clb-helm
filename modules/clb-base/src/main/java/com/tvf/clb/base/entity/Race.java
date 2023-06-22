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
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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
    @Id
    private Long id;
    @Transient
    private String raceId;
    @Transient
    private String meetingUUID;
    @Transient
    private String raceType;
    private Long meetingId;
    private String name;
    private Integer number;
    private Instant advertisedStart;
    private Instant actualStart;
    @JsonSerialize(using = PgJsonObjectSerializer.class)
    @JsonDeserialize(using = PgJsonObjectDeserializer.class)
    private Json marketIds;
    private String mainMarketStatusId;
    private Json resultsDisplay;
    private String status;
    private Integer distance;
    @Transient
    private String raceSiteUrl;
    @Transient
    private String meetingName;
    private String silkUrl;
    private String fullFormUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Race)) return false;

        Race race = (Race) o;

        if (!Objects.equals(name, race.name)) return false;
        if (!Objects.equals(number, race.number)) return false;
        return Objects.equals(advertisedStart, race.advertisedStart);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (advertisedStart != null ? advertisedStart.hashCode() : 0);
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
