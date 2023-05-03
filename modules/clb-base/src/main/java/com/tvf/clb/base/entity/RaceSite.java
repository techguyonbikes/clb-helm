package com.tvf.clb.base.entity;


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
@Table("race_site")
public class RaceSite {
    private Long id;
    private Long generalRaceId;
    private Integer siteId;
    private String raceSiteId;
    private Instant startDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RaceSite)) return false;

        RaceSite raceSite = (RaceSite) o;

        if (!Objects.equals(generalRaceId, raceSite.generalRaceId))
            return false;
        return Objects.equals(siteId, raceSite.siteId);
    }

    @Override
    public int hashCode() {
        int result = generalRaceId != null ? generalRaceId.hashCode() : 0;
        result = 31 * result + (siteId != null ? siteId.hashCode() : 0);
        return result;
    }
}
