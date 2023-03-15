package com.tvf.clb.base.entity;

import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("entrants")
public class EntrantRedis {

    private String entrantId;
    private Long raceId;
    private String name;
    private Integer number;
    private String marketId;
    private String status;
    private Map<Integer, List<Float>> priceFluctuations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntrantRedis)) return false;
        EntrantRedis that = (EntrantRedis) o;
        return Objects.equals(name, that.name)
                && Objects.equals(number, that.number)
                && Objects.equals(marketId, that.marketId)
                && Objects.equals(priceFluctuations, that.priceFluctuations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, number, marketId, priceFluctuations);
    }
}
