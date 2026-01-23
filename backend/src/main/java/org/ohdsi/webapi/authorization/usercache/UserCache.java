package org.ohdsi.webapi.authorization.usercache;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class UserCache {

    private final Logger logger = LoggerFactory.getLogger(UserCache.class);

    // Using a read-write lock to secure access to our resources.  This lock will
    // allow multiple readers but if a write lock is placed then all readers will be
    // blocked until the write lock is removed.
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final BiMap<Long,String> idLoginBiMap = HashBiMap.create();
    private final HashMap<Long, UserProfile> userProfiles = new HashMap<>();

    private final UserCacheLoader userCacheLoader;
    public UserCache(UserCacheLoader userCacheLoader) {
        this.userCacheLoader = userCacheLoader;
    }

    public void clear() {
        readWriteLock.writeLock().lock();
        try {
            idLoginBiMap.clear();
            userProfiles.clear();
            logger.info("User cache cleared");
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public UserProfile getUserProfile(Long userId) {
        readWriteLock.readLock().lock();
        try {
            var userProfile = userProfiles.get(userId);
            if (userProfile != null) {
                return userProfile;
            }
        } finally {
            readWriteLock.readLock().unlock();
        }

        // Doing the load outside of the write lock because it is a relatively
        // slow operation.
        var userProfile = userCacheLoader.loadUserProfile(userId);
        readWriteLock.writeLock().lock();
        try {
            userProfiles.put(userId, userProfile);
            logger.info("User profile cached for userId: {}", userId);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return userProfile;
    }

    public UserProfile getUserProfile(String login) {
        var idLoginBiMap = getIdLoginBiMap();
        var userId = idLoginBiMap.inverse().get(login);
        if (userId == null) {
            throw new NotFoundException(String.format("User with login %s not found", login));
        }

        return getUserProfile(userId);
    }

    public BiMap<Long,String> getIdLoginBiMap() {
        readWriteLock.readLock().lock();
        try {
            if (!idLoginBiMap.isEmpty()) {
                return idLoginBiMap;
            }
        } finally {
            readWriteLock.readLock().unlock();
        }

        readWriteLock.writeLock().lock();
        try {
            if (idLoginBiMap.isEmpty()) {
                idLoginBiMap.putAll(userCacheLoader.loadIdLoginBiMap());
            }
            return idLoginBiMap;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
