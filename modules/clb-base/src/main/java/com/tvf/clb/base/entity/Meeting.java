package com.tvf.clb.base.entity;

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
@Table("meeting")
public class Meeting {
    @Id
    private Long id;
    @Transient
    private String meetingId;
    private String name;
    private Instant advertisedDate;
    private String categoryId;
    private String venueId;
    private String trackCondition;
    private String country;
    private String state;
    private Boolean hasFixed;
    private String regionId;
    private String feedId;
    private String raceType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meeting)) return false;

        Meeting meeting = (Meeting) o;

        if (!Objects.equals(name, meeting.name)) return false;
        if (!Objects.equals(advertisedDate, meeting.advertisedDate))
            return false;
        if (!Objects.equals(categoryId, meeting.categoryId)) return false;
        if (!Objects.equals(venueId, meeting.venueId)) return false;
        if (!Objects.equals(trackCondition, meeting.trackCondition))
            return false;
        if (!Objects.equals(country, meeting.country)) return false;
        if (!Objects.equals(state, meeting.state)) return false;
        if (!Objects.equals(hasFixed, meeting.hasFixed)) return false;
        if (!Objects.equals(regionId, meeting.regionId)) return false;
        if (!Objects.equals(feedId, meeting.feedId)) return false;
        return Objects.equals(raceType, meeting.raceType);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (advertisedDate != null ? advertisedDate.hashCode() : 0);
        result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
        result = 31 * result + (venueId != null ? venueId.hashCode() : 0);
        result = 31 * result + (trackCondition != null ? trackCondition.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (hasFixed != null ? hasFixed.hashCode() : 0);
        result = 31 * result + (regionId != null ? regionId.hashCode() : 0);
        result = 31 * result + (feedId != null ? feedId.hashCode() : 0);
        result = 31 * result + (raceType != null ? raceType.hashCode() : 0);
        return result;
    }
}
