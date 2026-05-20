package com.airbnb.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImageResponse {

    private Long id;
    private String imagePath;
    private Boolean isPrimary;
    private Integer displayOrder;
}