package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.ChangePasswordRequest;
import com.airbnb.backend.dto.request.UpdateProfileRequest;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PublicUserResponse;
import com.airbnb.backend.dto.response.UserResponse;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.UserRepository;
import com.airbnb.backend.util.AppConstants;
import com.airbnb.backend.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageUtil fileStorageUtil;
    private final PasswordEncoder passwordEncoder;


    public UserResponse getUserById(Long userId) {
        return mapToResponse(findUser(userId));
    }


    public PublicUserResponse getPublicUserById(Long userId) {
        return mapToPublicResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getBio() != null) user.setBio(request.getBio());

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = findUser(userId);

        String newPath = fileStorageUtil.saveFile(file, AppConstants.USER_IMAGE_DIR);
        String oldPath = user.getProfileImagePath();
        user.setProfileImagePath(newPath);
        User saved = userRepository.save(user);

        if (oldPath != null) fileStorageUtil.deleteFile(oldPath);
        return mapToResponse(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (user.getPassword() == null) {
            throw new BadRequestException(
                    "Your account uses Google sign-in and has no password to change");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }


    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by("createdAt").descending()));

        return PagedResponse.<UserResponse>builder()
                .content(users.getContent().stream().map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .pageNumber(users.getNumber())
                .pageSize(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .first(users.isFirst())
                .build();
    }

    @Transactional
    public UserResponse setUserActive(Long adminId, Long userId, boolean active) {
        if (adminId.equals(userId)) {
            throw new BadRequestException("You cannot change your own active status");
        }
        User user = findUser(userId);
        user.setIsActive(active);
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

    public PublicUserResponse mapToPublicResponse(User user) {
        return PublicUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .profileImagePath(user.getProfileImagePath())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}