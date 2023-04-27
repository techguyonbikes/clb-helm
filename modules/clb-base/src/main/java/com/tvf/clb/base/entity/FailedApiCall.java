package com.tvf.clb.base.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("failed_api_call")
public class FailedApiCall {
    @Id
    private Long id;
    private String className;
    private String methodName;
    private String params; // save class name and value of the params in target method
    private Instant failedTime;

}
