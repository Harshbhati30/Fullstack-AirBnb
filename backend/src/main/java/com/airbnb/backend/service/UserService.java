package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.UpdateProfileRequest;
import com.airbnb.backend.dto.response.UserResponse;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.UserRepository;
import com.airbnb.backend.util.AppConstants;
import com.airbnb.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageUtil fileStorageUtil;

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", userId));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId,
                                      UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", userId));

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());
        if (request.getBio() != null)
            user.setBio(request.getBio());

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", userId));

        if (user.getProfileImagePath() != null) {
            fileStorageUtil.deleteFile(user.getProfileImagePath());
        }

        String imagePath = fileStorageUtil.saveFile(
                file, AppConstants.USER_IMAGE_DIR);
        user.setProfileImagePath(imagePath);

        return mapToResponse(userRepository.save(user));
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .bio(user.getBio())
                .profileImagePath(user.getProfileImagePath())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}