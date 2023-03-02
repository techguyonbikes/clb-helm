package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Meeting;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MeetingRepository extends R2dbcRepository<Meeting, UUID> {
}
