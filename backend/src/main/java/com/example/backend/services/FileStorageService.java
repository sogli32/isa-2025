package com.example.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String uploadDir = "uploads/";
    private final String videoDir = uploadDir + "videos/";
    private final String thumbnailDir = uploadDir + "thumbnails/";

    public FileStorageService() {
        // Kreiranje direktorijuma pri pokretanju aplikacije
        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(videoDir));
            Files.createDirectories(Paths.get(thumbnailDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    /**
     * Upload videa - max 200MB, samo mp4 format
     */
    public String uploadVideo(MultipartFile file) throws IOException {
        // Validacija
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > 200 * 1024 * 1024) { // 200 MB
            throw new IllegalArgumentException("File size exceeds 200MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("video/mp4")) {
            throw new IllegalArgumentException("Only MP4 video format is allowed");
        }

        // Generisanje unique filename
        String filename = UUID.randomUUID().toString() + ".mp4";
        Path filePath = Paths.get(videoDir + filename);

        // Upload sa timeout simulacijom (u realnom scenariju bi ovde bio timer)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filename;
    }

    /**
     * Upload thumbnail slike sa kompresijom
     */
    public String uploadThumbnail(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Thumbnail file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Only image files are allowed for thumbnail");
        }

        // Čitanje originalne slike
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

        // Kompresija slike (resize to 640x360 for thumbnail)
        BufferedImage compressedImage = compressImage(originalImage, 640, 360);

        // Generisanje unique filename
        String filename = UUID.randomUUID().toString() + ".jpg";
        Path filePath = Paths.get(thumbnailDir + filename);

        // Čuvanje kompresovane slike
        ImageIO.write(compressedImage, "jpg", filePath.toFile());

        return filename;
    }

    /**
     * Kompresija slike na željene dimenzije
     */
    private BufferedImage compressImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        return resizedImage;
    }

    /**
     * Brisanje fajla
     */
    public void deleteFile(String filename, boolean isVideo) {
        try {
            String directory = isVideo ? videoDir : thumbnailDir;
            Path filePath = Paths.get(directory + filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + filename, e);
        }
    }

    /**
     * Učitavanje fajla kao byte array
     */
    public byte[] loadFile(String filename, boolean isVideo) throws IOException {
        String directory = isVideo ? videoDir : thumbnailDir;
        Path filePath = Paths.get(directory + filename);
        return Files.readAllBytes(filePath);
    }

    /**
     * Provera da li fajl postoji
     */
    public boolean fileExists(String filename, boolean isVideo) {
        String directory = isVideo ? videoDir : thumbnailDir;
        Path filePath = Paths.get(directory + filename);
        return Files.exists(filePath);
    }
}
