package net.orbismc.omcapi.object.mcmmo;

import java.util.UUID;

public class SkillsContext {
    private final String playerIdentifier; // Can be either a name or UUID
    private final boolean includeAllSkills;

    public SkillsContext(String playerIdentifier, boolean includeAllSkills) {
        this.playerIdentifier = playerIdentifier;
        this.includeAllSkills = includeAllSkills;
    }

    public String getPlayerIdentifier() {
        return playerIdentifier;
    }

    public boolean shouldIncludeAllSkills() {
        return includeAllSkills;
    }

    /**
     * Check if the player identifier is a UUID
     * @return true if the playerIdentifier is a valid UUID
     */
    public boolean isUUID() {
        try {
            UUID.fromString(playerIdentifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}