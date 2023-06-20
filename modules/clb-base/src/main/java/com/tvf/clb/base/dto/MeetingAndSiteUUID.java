package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Meeting;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class MeetingAndSiteUUID extends Meeting {
    private String siteUUID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingAndSiteUUID)) return false;
        if (!super.equals(o)) return false;
        MeetingAndSiteUUID that = (MeetingAndSiteUUID) o;
        return Objects.equals(siteUUID, that.siteUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), siteUUID);
    }
}
