package com.airbnb.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlists",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_wishlist_user_property",
                        columnNames = {"user_id", "property_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}