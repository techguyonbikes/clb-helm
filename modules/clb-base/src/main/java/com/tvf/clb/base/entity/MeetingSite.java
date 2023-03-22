package com.tvf.clb.base.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("meeting_site")
public class MeetingSite {
    @Id
    private Long id;
    private Long generalMeetingId;
    private String meetingSiteId;
    private Integer siteId;
    private Instant startDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingSite that = (MeetingSite) o;
        return Objects.equals(siteId, that.siteId) && Objects.equals(generalMeetingId, that.generalMeetingId) && Objects.equals(meetingSiteId, that.meetingSiteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generalMeetingId, meetingSiteId, siteId);
    }
}
