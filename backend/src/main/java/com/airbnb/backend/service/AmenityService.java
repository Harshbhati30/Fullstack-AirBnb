package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.AmenityRequest;
import com.airbnb.backend.dto.response.AmenityResponse;
import com.airbnb.backend.entity.Amenity;
import com.airbnb.backend.exception.ConflictException;
import com.airbnb.backend.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmenityResponse createAmenity(AmenityRequest request) {
        String name = request.getName().trim();
        if (amenityRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("Amenity already exists: " + name);
        }
        Amenity saved = amenityRepository.save(
                Amenity.builder().name(name).icon(request.getIcon()).build());
        return mapToResponse(saved);
    }

    private AmenityResponse mapToResponse(Amenity amenity) {
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }
}