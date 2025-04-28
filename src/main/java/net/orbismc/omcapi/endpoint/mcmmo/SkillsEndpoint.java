package net.orbismc.omcapi.endpoint.mcmmo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.javalin.http.BadRequestResponse;
import net.orbismc.omcapi.object.endpoint.PostEndpoint;
import net.orbismc.omcapi.object.mcmmo.SkillsContext;
import net.orbismc.omcapi.util.JSONUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class SkillsEndpoint extends PostEndpoint<SkillsContext> {

    @Override
    public SkillsContext getObjectOrNull(JsonElement element) {
        JsonObject jsonObject = JSONUtil.getJsonElementAsJsonObjectOrNull(element);
        if (jsonObject == null) throw new BadRequestResponse("Your query contains a value that is not a JSON object");

        JsonElement playerElement = jsonObject.get("player");
        JsonElement allSkillsElement = jsonObject.get("all_skills");

        if (playerElement == null) throw new BadRequestResponse("Missing 'player' parameter");

        String player = JSONUtil.getJsonElementAsStringOrNull(playerElement);
        if (player == null) throw new BadRequestResponse("Invalid 'player' parameter");

        boolean includeAllSkills = false;
        if (allSkillsElement != null) {
            try {
                includeAllSkills = allSkillsElement.getAsBoolean();
            } catch (Exception e) {
                // Default to false if there's any problem parsing
            }
        }

        return new SkillsContext(player, includeAllSkills);
    }

    @Override
    public JsonElement getJsonElement(SkillsContext context) {
        JsonObject skillsObject = new JsonObject();

        // Get the player either by name or UUID
        OfflinePlayer offlinePlayer;
        if (context.isUUID()) {
            try {
                UUID uuid = UUID.fromString(context.getPlayerIdentifier());
                offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            } catch (IllegalArgumentException e) {
                throw new BadRequestResponse("Invalid UUID format");
            }
        } else {
            offlinePlayer = Bukkit.getOfflinePlayer(context.getPlayerIdentifier());
        }

        // If player doesn't exist or has never played
        if (!offlinePlayer.hasPlayedBefore()) {
            throw new BadRequestResponse("Player not found or has never played");
        }

        // Add basic player info
        skillsObject.addProperty("name", offlinePlayer.getName());
        skillsObject.addProperty("uuid", offlinePlayer.getUniqueId().toString());

        // Get mcMMO data using reflection to avoid direct dependencies
        try {
            // Get the mcMMO API classes
            Class<?> skillAPIClass = Class.forName("com.gmail.nossr50.api.SkillAPI");
            Class<?> userManagerClass = Class.forName("com.gmail.nossr50.util.player.UserManager");
            Class<?> primarySkillTypeClass = Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType");

            // Get the mcMMO player
            Object mcMMOPlayer = userManagerClass.getMethod("getOfflinePlayer", OfflinePlayer.class)
                    .invoke(null, offlinePlayer);

            // If mcMMO player doesn't exist
            if (mcMMOPlayer == null) {
                skillsObject.addProperty("has_mcmmo_data", false);
                return skillsObject;
            }

            skillsObject.addProperty("has_mcmmo_data", true);

            // Add power level
            int powerLevel = (int) mcMMOPlayer.getClass().getMethod("getPowerLevel").invoke(mcMMOPlayer);
            skillsObject.addProperty("power_level", powerLevel);

            // Add skill data
            JsonObject skillsData = new JsonObject();

            // Get all skills
            List<String> allSkills;
            if (context.shouldIncludeAllSkills()) {
                allSkills = (List<String>) skillAPIClass.getMethod("getSkills").invoke(null);
            } else {
                allSkills = (List<String>) skillAPIClass.getMethod("getNonChildSkills").invoke(null);
            }

            // Add each skill's data
            for (String skillName : allSkills) {
                Object skillType = Enum.valueOf(
                        (Class<Enum>) primarySkillTypeClass,
                        skillName);

                JsonObject skillObject = new JsonObject();

                int skillLevel = (int) mcMMOPlayer.getClass()
                        .getMethod("getSkillLevel", primarySkillTypeClass)
                        .invoke(mcMMOPlayer, skillType);

                int skillXp = (int) mcMMOPlayer.getClass()
                        .getMethod("getSkillXpLevel", primarySkillTypeClass)
                        .invoke(mcMMOPlayer, skillType);

                int xpToNextLevel = (int) mcMMOPlayer.getClass()
                        .getMethod("getXpToLevel", primarySkillTypeClass)
                        .invoke(mcMMOPlayer, skillType);

                skillObject.addProperty("level", skillLevel);
                skillObject.addProperty("xp", skillXp);
                skillObject.addProperty("xp_to_next_level", xpToNextLevel);

                skillsData.add(skillName, skillObject);
            }

            skillsObject.add("skills", skillsData);

            // Add skill categories
            JsonObject categoriesObject = new JsonObject();

            // Combat skills
            JsonArray combatSkills = new JsonArray();
            List<String> combatSkillsList = (List<String>) skillAPIClass.getMethod("getCombatSkills").invoke(null);
            for (String skill : combatSkillsList) {
                combatSkills.add(skill);
            }
            categoriesObject.add("combat", combatSkills);

            // Gathering skills
            JsonArray gatheringSkills = new JsonArray();
            List<String> gatheringSkillsList = (List<String>) skillAPIClass.getMethod("getGatheringSkills").invoke(null);
            for (String skill : gatheringSkillsList) {
                gatheringSkills.add(skill);
            }
            categoriesObject.add("gathering", gatheringSkills);

            // Misc skills
            JsonArray miscSkills = new JsonArray();
            List<String> miscSkillsList = (List<String>) skillAPIClass.getMethod("getMiscSkills").invoke(null);
            for (String skill : miscSkillsList) {
                miscSkills.add(skill);
            }
            categoriesObject.add("misc", miscSkills);

            skillsObject.add("categories", categoriesObject);

        } catch (Exception e) {
            // If we encounter any error with mcMMO
            skillsObject.addProperty("has_mcmmo_data", false);
            skillsObject.addProperty("error", "Failed to retrieve mcMMO data: " + e.getMessage());
        }

        return skillsObject;
    }
}