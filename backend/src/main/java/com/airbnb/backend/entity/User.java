
package com.airbnb.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /*
     * Password is nullable because OAuth2 users (Google login)
     * don't have a password. They authenticate via Google.
     * We distinguish them using the 'provider' field below.
     */
    @Column(length = 255)
    private String password;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    @Column(name = "bio", length = 500)
    private String bio;

    /*
     * OAuth2 provider tracking.            fvfvy==* "LOCAL" = registered via email/password
             * "GOOGLE" = registered via Google OAuth2
       * We need this to prevent a user who signed up with Google
     * from trying to log in with email/password (and vice versa).
     */


    @Column(name = "provider", length = 20)
    @Builder.Default
    private String provider = "LOCAL";

    @Column(name = "provider_id")
    private String providerId; // Google's unique user ID

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();


    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Property> properties = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Wishlist> wishlist = new ArrayList<>();

    // Convenience helper — used in auth checks
    public String getFullName() {
        return firstName + " " + lastName;
    }
}