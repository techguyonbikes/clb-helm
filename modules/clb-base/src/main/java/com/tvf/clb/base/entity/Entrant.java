package com.tvf.clb.base.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tvf.clb.base.utils.CommonUtils;
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
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tvf.clb.base.utils.AppConstant.LAD_BROKE_SITE_ID;


@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("entrant")
@Slf4j
public class Entrant {
    @Id
    private Long id;
    @Transient
    private String entrantId;
    @Transient
    private String raceUUID;
    private Long raceId;
    private String name;
    private Integer number;
    private Integer barrier;
    private boolean visible;
    private String marketId;
    private boolean isScratched;
    private Instant scratchedTime;
    @JsonSerialize(using = PgJsonObjectSerializer.class)
    @JsonDeserialize(using = PgJsonObjectDeserializer.class)
    private Json priceFluctuations;
    @Transient
    private List<Float> currentSitePrice;
    private Integer position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entrant)) return false;

        Entrant entrant = (Entrant) o;

        if (visible != entrant.visible) return false;
        if (!Objects.equals(raceId, entrant.raceId)) return false;
        if (!Objects.equals(name, entrant.name)) return false;
        if (!Objects.equals(number, entrant.number)) return false;
        if (!Objects.equals(barrier, entrant.barrier)) return false;

        Map<Integer, List<Float>> prices = CommonUtils.getSitePriceFromJsonb(priceFluctuations);
        Map<Integer, List<Float>> entrantPrices = CommonUtils.getSitePriceFromJsonb(entrant.getPriceFluctuations());

        if ((prices == null || entrantPrices == null) || !compareMaps(prices, entrantPrices)) return false;
        if (isScratched != entrant.isScratched) return false;
        if (!Objects.equals(scratchedTime, entrant.scratchedTime)) return false;
        if (!Objects.equals(position, entrant.position)) return false;
        return Objects.equals(marketId, entrant.marketId);
    }


    public boolean compareMaps(Map<Integer, List<Float>> map1, Map<Integer, List<Float>> map2) {
        if (map1.size() != map2.size()) {
            return false;
        }
        for (int key : map1.keySet()) {
            if (!map2.containsKey(key)) {
                return false;
            }
            List<Float> list1 = map1.get(key);
            List<Float> list2 = map2.get(key);
            if (!list1.equals(list2)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public int hashCode() {
        int result = raceId != null ? raceId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (barrier != null ? barrier.hashCode() : 0);
        result = 31 * result + (visible ? 1 : 0);
        result = 31 * result + (marketId != null ? marketId.hashCode() : 0);
        result = 31 * result + (isScratched ? 1 : 0);
        result = 31 * result + (isScratched ? 1 : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }

    public Map<Integer, List<Float>> getPrices() {
        return CommonUtils.getSitePriceFromJsonb(priceFluctuations);
    }

}
