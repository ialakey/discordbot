package com.alakey.discordbot.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BlockedEntityService {

    private final List<String> blockedRoles = new CopyOnWriteArrayList<>();
    private final List<String> blockedNames = new CopyOnWriteArrayList<>();

    public List<String> getBlockedRoles() {
        return blockedRoles;
    }

    public List<String> getBlockedNames() {
        return blockedNames;
    }

    public void addBlockedRole(String roleName) {
        if (!blockedRoles.contains(roleName)) {
            blockedRoles.add(roleName);
        }
    }

    public void removeBlockedRole(String roleName) {
        blockedRoles.remove(roleName);
    }

    public void addBlockedName(String username) {
        if (!blockedNames.contains(username)) {
            blockedNames.add(username);
        }
    }

    public void removeBlockedName(String username) {
        blockedNames.remove(username);
    }
}