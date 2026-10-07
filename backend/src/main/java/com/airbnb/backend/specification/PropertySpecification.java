package com.airbnb.backend.specification;

import com.airbnb.backend.dto.request.PropertySearchRequest;
import com.airbnb.backend.entity.Address;
import com.airbnb.backend.entity.Amenity;
import com.airbnb.backend.entity.Booking;
import com.airbnb.backend.entity.Property;
import com.airbnb.backend.enums.BookingStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class PropertySpecification {

    private PropertySpecification() {}

    public static Specification<Property> build(PropertySearchRequest r) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isActive")));

            if (StringUtils.hasText(r.getCity())) {
                predicates.add(cb.like(cb.lower(root.get("city")),
                        "%" + r.getCity().trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(r.getLocation())) {
                String like = "%" + r.getLocation().trim().toLowerCase() + "%";
                Join<Property, Address> address = root.join("address", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(address.get("city")), like),
                        cb.like(cb.lower(address.get("state")), like),
                        cb.like(cb.lower(address.get("country")), like),
                        cb.like(cb.lower(root.get("title")), like)
                ));
            }

            if (r.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<BigDecimal>get("pricePerNight"), r.getMinPrice()));
            }
            if (r.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<BigDecimal>get("pricePerNight"), r.getMaxPrice()));
            }
            if (r.getGuests() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<Integer>get("maxGuests"), r.getGuests()));
            }
            if (r.getPropertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), r.getPropertyType()));
            }
            if (r.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<BigDecimal>get("averageRating"),
                        BigDecimal.valueOf(r.getMinRating())));
            }

            if (r.getAmenityIds() != null) {
                for (Long amenityId : r.getAmenityIds()) {
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<Property> sp = sub.from(Property.class);
                    Join<Property, Amenity> aj = sp.join("amenities");
                    sub.select(sp.<Long>get("id")).where(
                            cb.equal(sp.get("id"), root.get("id")),
                            cb.equal(aj.get("id"), amenityId));
                    predicates.add(cb.exists(sub));
                }
            }

            if (r.getCheckIn() != null && r.getCheckOut() != null
                    && r.getCheckOut().isAfter(r.getCheckIn())) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Booking> b = sub.from(Booking.class);
                sub.select(b.<Long>get("id")).where(
                        cb.equal(b.get("property").get("id"), root.get("id")),
                        b.get("status").in(BookingStatus.PENDING, BookingStatus.CONFIRMED),
                        cb.lessThan(b.<java.time.LocalDate>get("checkInDate"), r.getCheckOut()),
                        cb.greaterThan(b.<java.time.LocalDate>get("checkOutDate"), r.getCheckIn()));
                predicates.add(cb.not(cb.exists(sub)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}