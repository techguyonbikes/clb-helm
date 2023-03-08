package com.tvf.clb.base.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.Gson;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;


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
    private String entrantId;
    private String raceId;
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
    private Integer position;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entrant)) return false;

        Entrant entrant = (Entrant) o;

        if (visible != entrant.visible) return false;
        if (!Objects.equals(entrantId, entrant.entrantId)) return false;
        if (!Objects.equals(raceId, entrant.raceId)) return false;
        if (!Objects.equals(name, entrant.name)) return false;
        if (!Objects.equals(number, entrant.number)) return false;
        if (!Objects.equals(barrier, entrant.barrier)) return false;
        Gson gson = new Gson();
        ArrayList<Double> prices = gson.fromJson(priceFluctuations.asString(), ArrayList.class);
        ArrayList<Double> entrantPrices = gson.fromJson(entrant.priceFluctuations.asString(), ArrayList.class);
        if ((prices == null || entrantPrices == null) || !compareArrayLists(prices, entrantPrices)) return false;
        if (isScratched != entrant.isScratched) return false;
        if (!Objects.equals(scratchedTime, entrant.scratchedTime)) return false;
        if (!Objects.equals(position, entrant.position)) return false;
        return Objects.equals(marketId, entrant.marketId);
    }

    public boolean compareArrayLists(ArrayList<Double> list1, ArrayList<Double> list2) {
        // Check if both ArrayLists have the same size
        if (list1.size() != list2.size()) {
            return false;
        }

        // Iterate through the ArrayLists and compare the elements at each index
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }

        // All elements are equal, so the ArrayLists are equal
        return true;
    }


    @Override
    public int hashCode() {
        int result = entrantId != null ? entrantId.hashCode() : 0;
        result = 31 * result + (raceId != null ? raceId.hashCode() : 0);
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
}
