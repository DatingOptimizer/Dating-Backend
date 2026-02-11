package com.groupf.dating.util;

import com.groupf.dating.common.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
public class ImageUtil {

    private static final int MAX_IMAGE_DIMENSION = 1568; // Claude's recommended max dimension

    private ImageUtil() {
        // Prevent instantiation
    }

    /**
     * Validates if the file is a valid image
     */
    public static boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (!AppConstants.ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return false;
        }

        return file.getSize() <= AppConstants.PHOTO_MAX_SIZE_BYTES;
    }

    /**
     * Validates image file extension
     */
    public static boolean isValidImageExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        return AppConstants.ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * Converts MultipartFile to Base64 string
     */
    public static String convertToBase64(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();

        // Resize if necessary
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image != null && (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION)) {
            image = resizeImage(image, MAX_IMAGE_DIMENSION);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = getImageFormat(file.getContentType());
            ImageIO.write(image, format, baos);
            imageBytes = baos.toByteArray();
        }

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Resizes image while maintaining aspect ratio
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int maxDimension) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            double aspectRatio = (double) originalWidth / originalHeight;

            if (originalWidth > originalHeight) {
                newWidth = maxDimension;
                newHeight = (int) (maxDimension / aspectRatio);
            } else {
                newHeight = maxDimension;
                newWidth = (int) (maxDimension * aspectRatio);
            }
        }

        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * Gets image format from content type
     */
    private static String getImageFormat(String contentType) {
        if (contentType != null && contentType.contains("png")) {
            return "png";
        }
        return "jpg";
    }

    /**
     * Gets file extension from filename
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Gets media type from content type (for Claude API)
     */
    public static String getMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.contains("png")) {
            return "image/png";
        }
        return "image/jpeg";
    }
}
