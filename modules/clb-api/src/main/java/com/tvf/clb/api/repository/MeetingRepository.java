package com.tvf.clb.api.repository;

import com.tvf.clb.base.entity.Meeting;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface MeetingRepository extends R2dbcRepository<Meeting, UUID> {
}
