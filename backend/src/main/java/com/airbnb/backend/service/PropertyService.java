package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.PropertyRequest;
import com.airbnb.backend.dto.request.PropertySearchRequest;
import com.airbnb.backend.dto.response.*;
import com.airbnb.backend.entity.*;
import com.airbnb.backend.enums.BookingStatus;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.exception.UnauthorizedException;
import com.airbnb.backend.repository.AmenityRepository;
import com.airbnb.backend.repository.BookingRepository;
import com.airbnb.backend.repository.PropertyRepository;
import com.airbnb.backend.repository.UserRepository;
import com.airbnb.backend.specification.PropertySpecification;
import com.airbnb.backend.util.AppConstants;
import com.airbnb.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    // sortBy comes from the URL, so it is whitelisted (an unknown field would 500,
    // and a path like host.password must never reach Sort.by)
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "pricePerNight", "averageRating", "title", "maxGuests");
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_IMAGES = 10;
    private static final List<BookingStatus> OPEN_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final BookingRepository bookingRepository;
    private final FileStorageUtil fileStorageUtil;
    private final UserService userService;

    // ------------------------------------------------------------------ create / read

    @Transactional
    public PropertyResponse createProperty(Long hostId, PropertyRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", hostId));

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
        return mapToResponse(propertyRepository.save(property));
    }

    /** Public: deactivated listings look like they don't exist. */
    public PropertyResponse getPropertyById(Long propertyId) {
        Property property = findProperty(propertyId);
        if (!Boolean.TRUE.equals(property.getIsActive())) {
            throw new ResourceNotFoundException("Property", "id", propertyId);
        }
        return mapToResponse(property);
    }

    public PagedResponse<PropertyResponse> getAllProperties(
            int page, int size, String sortBy, String sortDir) {
        return searchProperties(new PropertySearchRequest(), page, size, sortBy, sortDir);
    }

    public PagedResponse<PropertyResponse> searchProperties(
            PropertySearchRequest filters, int page, int size, String sortBy, String sortDir) {

        String sortField = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);

        return buildPagedResponse(
                propertyRepository.findAll(PropertySpecification.build(filters), pageable));
    }

    /** Host dashboard: ALL of the host's listings, including deactivated ones. */
    public PagedResponse<PropertyResponse> getHostProperties(Long hostId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("createdAt").descending());
        return buildPagedResponse(propertyRepository.findByHostId(hostId, pageable));
    }

    /** Admin: every property, active or not. */
    public PagedResponse<PropertyResponse> getAllPropertiesForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("createdAt").descending());
        return buildPagedResponse(propertyRepository.findAll(pageable));
    }

    // ------------------------------------------------------------------ update / status / delete

    @Transactional
    public PropertyResponse updateProperty(Long userId, boolean isAdmin,
                                           Long propertyId, PropertyRequest request) {
        Property property = getManagedProperty(userId, isAdmin, propertyId);

        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setPricePerNight(request.getPricePerNight());
        property.setMaxGuests(request.getMaxGuests());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setCity(request.getCity());
        property.setPropertyType(request.getPropertyType());

        Address address = property.getAddress();
        if (address == null) {
            address = new Address();
            property.setAddress(address);
        }
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setZipCode(request.getZipCode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        if (request.getAmenityIds() != null) {
            List<Amenity> amenities = amenityRepository.findByIdIn(request.getAmenityIds());
            property.getAmenities().clear();
            property.getAmenities().addAll(amenities);
        }

        return mapToResponse(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse setActive(Long userId, boolean isAdmin, Long propertyId, boolean active) {
        Property property = getManagedProperty(userId, isAdmin, propertyId);

        if (!active) {
            boolean hasOpenBookings = bookingRepository
                    .existsByPropertyIdAndStatusInAndCheckOutDateAfter(
                            propertyId, OPEN_BOOKING_STATUSES, LocalDate.now().minusDays(1));
            if (hasOpenBookings) {
                throw new BadRequestException(
                        "This property has upcoming bookings. Cancel them before deactivating it.");
            }
        }

        property.setIsActive(active);
        return mapToResponse(propertyRepository.save(property));
    }


    @Transactional
    public void deleteProperty(Long userId, boolean isAdmin, Long propertyId) {
        setActive(userId, isAdmin, propertyId, false);
    }



    @Transactional
    public List<PropertyImageResponse> uploadPropertyImages(
            Long userId, boolean isAdmin, Long propertyId, List<MultipartFile> files) {

        Property property = getManagedProperty(userId, isAdmin, propertyId);

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Please select at least one image");
        }
        int existing = property.getImages().size();
        if (existing + files.size() > MAX_IMAGES) {
            throw new BadRequestException("Maximum " + MAX_IMAGES + " images allowed per property ("
                    + existing + " already uploaded)");
        }

        files.forEach(fileStorageUtil::validateImage);

        boolean hasPrimary = property.getImages().stream()
                .anyMatch(i -> Boolean.TRUE.equals(i.getIsPrimary()));

        List<PropertyImage> newImages = new ArrayList<>();
        int order = existing;
        for (MultipartFile file : files) {
            String path = fileStorageUtil.saveFile(file, AppConstants.PROPERTY_IMAGE_DIR);
            newImages.add(PropertyImage.builder()
                    .imagePath(path)
                    .property(property)
                    .isPrimary(false)
                    .displayOrder(order++)
                    .build());
        }
        if (!hasPrimary) {
            newImages.get(0).setIsPrimary(true);
        }

        property.getImages().addAll(newImages);
        propertyRepository.saveAndFlush(property);

        return newImages.stream().map(this::mapImageToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteImage(Long userId, boolean isAdmin, Long propertyId, Long imageId) {
        Property property = getManagedProperty(userId, isAdmin, propertyId);

        PropertyImage image = property.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String path = image.getImagePath();

        property.getImages().remove(image);   // orphanRemoval deletes the row

        if (wasPrimary && !property.getImages().isEmpty()) {
            property.getImages().get(0).setIsPrimary(true);
        }
        propertyRepository.save(property);
        fileStorageUtil.deleteFile(path);
    }

    @Transactional
    public List<PropertyImageResponse> setPrimaryImage(
            Long userId, boolean isAdmin, Long propertyId, Long imageId) {

        Property property = getManagedProperty(userId, isAdmin, propertyId);

        boolean found = property.getImages().stream().anyMatch(i -> i.getId().equals(imageId));
        if (!found) {
            throw new ResourceNotFoundException("Image", "id", imageId);
        }
        property.getImages().forEach(i -> i.setIsPrimary(i.getId().equals(imageId)));
        propertyRepository.save(property);

        return property.getImages().stream().map(this::mapImageToResponse)
                .collect(Collectors.toList());
    }


    private Property findProperty(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));
    }


    private Property getManagedProperty(Long userId, boolean isAdmin, Long propertyId) {
        Property property = findProperty(propertyId);
        if (!isAdmin && !property.getHost().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to modify this property");
        }
        return property;
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
                .host(userService.mapToPublicResponse(property.getHost()))
                .images(property.getImages().stream()
                        .sorted(Comparator.comparing(PropertyImage::getDisplayOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
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

    private PagedResponse<PropertyResponse> buildPagedResponse(Page<Property> page) {
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