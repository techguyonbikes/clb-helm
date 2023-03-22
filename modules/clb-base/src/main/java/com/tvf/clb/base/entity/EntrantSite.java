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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;


@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("entrant_site")
@Slf4j
public class EntrantSite {
    @Id
    private Long id;
    private Long generalEntrantId;
    private String entrantSiteId;
    private Integer siteId;
    @JsonSerialize(using = PgJsonObjectSerializer.class)
    @JsonDeserialize(using = PgJsonObjectDeserializer.class)
    private Json priceFluctuations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntrantSite)) return false;

        EntrantSite that = (EntrantSite) o;

        if (!Objects.equals(generalEntrantId, that.generalEntrantId))
            return false;
        if (!Objects.equals(entrantSiteId, that.entrantSiteId))
            return false;
        if (!Objects.equals(siteId, that.siteId)) return false;
        return Objects.equals(priceFluctuations, that.priceFluctuations);
    }

    @Override
    public int hashCode() {
        int result = generalEntrantId != null ? generalEntrantId.hashCode() : 0;
        result = 31 * result + (entrantSiteId != null ? entrantSiteId.hashCode() : 0);
        result = 31 * result + (siteId != null ? siteId.hashCode() : 0);
        result = 31 * result + (priceFluctuations != null ? priceFluctuations.hashCode() : 0);
        return result;
    }
}
