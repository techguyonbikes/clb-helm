package com.tvf.clb.base.dto.sportbet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class SportBetSectionsDto {
    private List<SportBetMeetingDto> sections;
    private String meetingDate;
}
