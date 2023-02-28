package com.tvf.clb.api.repository;

import com.tvf.clb.base.model.Meeting;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface MeetingRepository extends R2dbcRepository<Meeting, UUID> {
}
