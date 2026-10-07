package com.airbnb.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String bio;
    private String profileImagePath;
    private LocalDateTime createdAt;
}