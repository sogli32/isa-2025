package com.example.backend.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache za thumbnail slike
 * Umesto da se svaki put čita sa file sistema, keširamo u memoriji
 */
@Service
public class ThumbnailCacheService {

    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();
    private final FileStorageService fileStorageService;

    public ThumbnailCacheService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Dobavi thumbnail iz keša, ili učitaj sa file sistema i keširaj
     */
    public byte[] getThumbnail(String filename) throws IOException {
        // Prvo proveri keš
        if (cache.containsKey(filename)) {
            return cache.get(filename);
        }

        // Ako nije u kešu, učitaj sa file sistema
        byte[] thumbnailData = fileStorageService.loadFile(filename, false);

        // Keširaj za sledeći put
        cache.put(filename, thumbnailData);

        return thumbnailData;
    }

    /**
     * Briši thumbnail iz keša
     */
    public void evict(String filename) {
        cache.remove(filename);
    }

    /**
     * Očisti kompletan keš
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Dobavi veličinu keša (za monitoring)
     */
    public int getCacheSize() {
        return cache.size();
    }
}
