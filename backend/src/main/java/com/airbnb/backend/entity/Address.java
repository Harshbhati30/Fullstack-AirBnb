
package com.airbnb.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "street", nullable = false)
    private String street;

    @NotBlank
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @NotBlank
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "zip_code", length = 20)
    private String zipCode;


    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;


    @OneToOne(mappedBy = "address")
    private Property property;
}