package com.airbnb.backend.util;


public final class AppConstants {

    private AppConstants() {}

    // Pagination defaults
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";

    // File upload
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/png", "image/webp", "image/jpg"
    };
    public static final String PROPERTY_IMAGE_DIR = "properties";
    public static final String USER_IMAGE_DIR = "users";

    // JWT
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_HOST = "ROLE_HOST";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}