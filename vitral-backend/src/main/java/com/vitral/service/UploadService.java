package com.vitral.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;

@Service
public class UploadService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path imageDirectory;

    public UploadService(@Value("${app.upload.image-dir:uploads/images}") String imageDirectory) {
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file) {
        validateImage(file);

        try {
            Files.createDirectories(imageDirectory);
            String filename = UUID.randomUUID() + extensionFor(file.getContentType());
            Path destination = imageDirectory.resolve(filename).normalize();
            if (!destination.startsWith(imageDirectory)) {
                throw badRequest("Nome de arquivo invalido.");
            }

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/api/v1/uploads/images/" + filename;
        } catch (IOException exception) {
            throw badRequest("Nao foi possivel salvar a imagem.");
        }
    }

    public Resource loadImage(String filename) {
        try {
            Path file = imageDirectory.resolve(filename).normalize();
            if (!file.startsWith(imageDirectory) || !Files.exists(file) || !Files.isRegularFile(file)) {
                throw new ResourceNotFoundException("Imagem nao encontrada.");
            }
            return new UrlResource(file.toUri());
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Imagem nao encontrada.");
        }
    }

    public String contentType(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) return "image/jpeg";
        if (normalized.endsWith(".png")) return "image/png";
        if (normalized.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Selecione uma imagem para upload.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw badRequest("A imagem deve ter no maximo 5MB.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw badRequest("Formato de imagem invalido. Use JPG, PNG ou WEBP.");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw badRequest("Formato de imagem invalido. Use JPG, PNG ou WEBP.");
        };
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}
