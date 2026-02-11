package com.groupf.dating.util;

import com.groupf.dating.common.AppConstants;

public class ValidationUtil {

    private ValidationUtil() {
        // Prevent instantiation
    }

    /**
     * Validates bio length
     */
    public static boolean isValidBioLength(String bio) {
        if (bio == null) {
            return false;
        }
        int length = bio.trim().length();
        return length >= AppConstants.BIO_MIN_LENGTH && length <= AppConstants.BIO_MAX_LENGTH;
    }

    /**
     * Validates photo count
     */
    public static boolean isValidPhotoCount(int count) {
        return count >= AppConstants.PHOTO_MIN_COUNT && count <= AppConstants.PHOTO_MAX_COUNT;
    }

    /**
     * Gets validation error message for bio
     */
    public static String getBioValidationError(String bio) {
        if (bio == null || bio.trim().isEmpty()) {
            return "Bio cannot be empty";
        }
        int length = bio.trim().length();
        if (length < AppConstants.BIO_MIN_LENGTH) {
            return AppConstants.ERROR_BIO_TOO_SHORT;
        }
        if (length > AppConstants.BIO_MAX_LENGTH) {
            return AppConstants.ERROR_BIO_TOO_LONG;
        }
        return null;
    }

    /**
     * Gets validation error message for photo count
     */
    public static String getPhotoCountValidationError(int count) {
        if (count < AppConstants.PHOTO_MIN_COUNT) {
            return AppConstants.ERROR_TOO_FEW_PHOTOS;
        }
        if (count > AppConstants.PHOTO_MAX_COUNT) {
            return AppConstants.ERROR_TOO_MANY_PHOTOS;
        }
        return null;
    }
}
