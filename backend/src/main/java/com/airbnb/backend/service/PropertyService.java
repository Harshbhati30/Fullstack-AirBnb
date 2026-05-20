package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.PropertyRequest;
import com.airbnb.backend.dto.response.*;
import com.airbnb.backend.entity.*;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.exception.UnauthorizedException;
import com.airbnb.backend.repository.*;
import com.airbnb.backend.util.AppConstants;
import com.airbnb.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageUtil fileStorageUtil;
    private final UserService userService;


    @Transactional
    public PropertyResponse createProperty(Long hostId,
                                           PropertyRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", hostId));

        Address address = Address.builder()
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .zipCode(request.getZipCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        List<Amenity> amenities = request.getAmenityIds() != null
                ? amenityRepository.findByIdIn(request.getAmenityIds())
                : List.of();

        Property property = Property.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .pricePerNight(request.getPricePerNight())
                .maxGuests(request.getMaxGuests())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .city(request.getCity())
                .propertyType(request.getPropertyType())
                .address(address)
                .host(host)
                .isActive(true)
                .build();

        property.getAmenities().addAll(amenities);
        Property saved = propertyRepository.save(property);
        return mapToResponse(saved);
    }

    public PropertyResponse getPropertyById(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", propertyId));
        return mapToResponse(property);
    }


    public PagedResponse<PropertyResponse> getAllProperties(
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Property> properties = propertyRepository
                .findAll(pageable);

        return buildPagedResponse(properties);
    }


    public PagedResponse<PropertyResponse> getHostProperties(
            Long hostId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<Property> properties = propertyRepository
                .findByHostIdAndIsActiveTrue(hostId, pageable);

        return buildPagedResponse(properties);
    }


    @Transactional
    public PropertyResponse updateProperty(Long hostId,
                                           Long propertyId,
                                           PropertyRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", propertyId));

        if (!property.getHost().getId().equals(hostId)) {
            throw new UnauthorizedException(
                    "You don't have permission to update this property");
        }

        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setPricePerNight(request.getPricePerNight());
        property.setMaxGuests(request.getMaxGuests());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setCity(request.getCity());
        property.setPropertyType(request.getPropertyType());

        // Update address
        Address address = property.getAddress();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setZipCode(request.getZipCode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        if (request.getAmenityIds() != null) {
            List<Amenity> amenities = amenityRepository
                    .findByIdIn(request.getAmenityIds());
            property.getAmenities().clear();
            property.getAmenities().addAll(amenities);
        }

        return mapToResponse(propertyRepository.save(property));
    }


    @Transactional
    public void deleteProperty(Long hostId, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", propertyId));

        if (!property.getHost().getId().equals(hostId)) {
            throw new UnauthorizedException(
                    "You don't have permission to delete this property");
        }

        property.setIsActive(false);
        propertyRepository.save(property);
    }


    @Transactional
    public List<PropertyImageResponse> uploadPropertyImages(
            Long hostId, Long propertyId, List<MultipartFile> files) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", propertyId));

        if (!property.getHost().getId().equals(hostId)) {
            throw new UnauthorizedException(
                    "You don't have permission to upload images for this property");
        }

        if (files.size() > 10) {
            throw new BadRequestException(
                    "Maximum 10 images allowed per property");
        }

        boolean hasImages = !property.getImages().isEmpty();
        int displayOrder = property.getImages().size();

        List<PropertyImage> savedImages = files.stream().map(file -> {
            String imagePath = fileStorageUtil.saveFile(
                    file, AppConstants.PROPERTY_IMAGE_DIR);

            return PropertyImage.builder()
                    .imagePath(imagePath)
                    .property(property)
                    .isPrimary(false)
                    .displayOrder(displayOrder)
                    .build();
        }).collect(Collectors.toList());

        if (!hasImages && !savedImages.isEmpty()) {
            savedImages.get(0).setIsPrimary(true);
        }

        property.getImages().addAll(savedImages);
        propertyRepository.save(property);

        return savedImages.stream()
                .map(this::mapImageToResponse)
                .collect(Collectors.toList());
    }


    public PropertyResponse mapToResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .city(property.getCity())
                .propertyType(property.getPropertyType())
                .averageRating(property.getAverageRating())
                .totalReviews(property.getTotalReviews())
                .isActive(property.getIsActive())
                .address(mapAddressToResponse(property.getAddress()))
                .host(userService.mapToResponse(property.getHost()))
                .images(property.getImages().stream()
                        .map(this::mapImageToResponse)
                        .collect(Collectors.toList()))
                .amenities(property.getAmenities().stream()
                        .map(this::mapAmenityToResponse)
                        .collect(Collectors.toSet()))
                .createdAt(property.getCreatedAt())
                .build();
    }

    private AddressResponse mapAddressToResponse(Address address) {
        if (address == null) return null;
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .zipCode(address.getZipCode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .build();
    }

    private PropertyImageResponse mapImageToResponse(PropertyImage image) {
        return PropertyImageResponse.builder()
                .id(image.getId())
                .imagePath(image.getImagePath())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    private AmenityResponse mapAmenityToResponse(Amenity amenity) {
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }

    private PagedResponse<PropertyResponse> buildPagedResponse(
            Page<Property> page) {
        List<PropertyResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<PropertyResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}