package tn.esprit.services.guardian.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache for API responses.
 * Location: Caches resource metadata, AI suggestions, and calendar events to reduce external calls.
 */
public class CacheProvider {

    public static <K, V> LoadingCache<K, V> createCache(com.github.benmanes.caffeine.cache.CacheLoader<K, V> loader) {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(loader);
    }
}
