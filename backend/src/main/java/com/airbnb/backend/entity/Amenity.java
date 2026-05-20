
package com.airbnb.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 100)
    private String icon; // icon name or CSS class for frontend


    @ManyToMany(mappedBy = "amenities")
    @Builder.Default
    private Set<Property> properties = new HashSet<>();
}