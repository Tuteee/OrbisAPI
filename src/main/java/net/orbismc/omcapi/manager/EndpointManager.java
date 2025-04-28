package net.orbismc.omcapi.manager;

import com.google.gson.*;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import net.orbismc.omcapi.endpoint.towny.list.NationsListEndpoint;
import net.orbismc.omcapi.endpoint.towny.list.PlayersListEndpoint;
import net.orbismc.omcapi.endpoint.towny.list.TownsListEndpoint;
import net.orbismc.omcapi.util.JSONUtil;
import net.milkbowl.vault.economy.Economy;
import net.orbismc.omcapi.endpoint.ServerEndpoint;
import net.orbismc.omcapi.endpoint.towny.NationsEndpoint;
import net.orbismc.omcapi.endpoint.towny.PlayersEndpoint;
import net.orbismc.omcapi.endpoint.towny.TownsEndpoint;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.UUID;

public class EndpointManager {

    private final Javalin javalin;
    private final Economy economy;

    public EndpointManager(Javalin javalin, FileConfiguration config, Economy economy) {
        this.javalin = javalin;
        this.economy = economy;
    }

    public void loadEndpoints() {
        ServerEndpoint serverEndpoint = new ServerEndpoint();
        javalin.get("/", ctx -> ctx.json(serverEndpoint.lookup()));

        loadPlayersEndpoint();
        loadTownsEndpoint();
        loadNationsEndpoint();
    }

    private JsonArray parseBody(String body) {
        JsonObject jsonObject = JSONUtil.getJsonObjectFromString(body);

        JsonArray queryArray = jsonObject.get("query").getAsJsonArray();
        if (queryArray == null) throw new BadRequestResponse("Invalid query array provided");

        return queryArray;
    }

    private void loadPlayersEndpoint() {
        PlayersListEndpoint ple = new PlayersListEndpoint();
        javalin.get("/players", ctx -> ctx.json(ple.lookup()));

        PlayersEndpoint playersEndpoint = new PlayersEndpoint(economy);
        javalin.post("/players", ctx -> {
            ctx.json(playersEndpoint.lookup(parseBody(ctx.body())));
        });

        // New endpoint for player skills with FIXED PATH PARAMETER SYNTAX
        javalin.get("/players/{playerIdentifier}/skills", ctx -> {
            String playerIdentifier = ctx.pathParam("playerIdentifier");

            // Create a response object
            JsonObject skillsResponse = new JsonObject();

            // Get the player
            OfflinePlayer offlinePlayer;
            try {
                // Try to parse as UUID first
                try {
                    UUID uuid = UUID.fromString(playerIdentifier);
                    offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                } catch (IllegalArgumentException e) {
                    // Not a UUID, assume it's a player name
                    offlinePlayer = Bukkit.getOfflinePlayer(playerIdentifier);
                }

                // Check if player exists
                if (!offlinePlayer.hasPlayedBefore()) {
                    throw new BadRequestResponse("Player not found or has never played");
                }

                // Add basic player info
                skillsResponse.addProperty("name", offlinePlayer.getName());
                skillsResponse.addProperty("uuid", offlinePlayer.getUniqueId().toString());

                try {
                    // Get mcMMO API classes via reflection
                    Class<?> skillAPIClass = Class.forName("com.gmail.nossr50.api.SkillAPI");
                    Class<?> userManagerClass = Class.forName("com.gmail.nossr50.util.player.UserManager");
                    Class<?> primarySkillTypeClass = Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType");

                    // Get mcMMO player
                    Object mcMMOPlayer = userManagerClass.getMethod("getOfflinePlayer", OfflinePlayer.class)
                            .invoke(null, offlinePlayer);

                    if (mcMMOPlayer == null) {
                        skillsResponse.addProperty("has_mcmmo_data", false);
                        ctx.json(skillsResponse.toString());
                        return;
                    }

                    skillsResponse.addProperty("has_mcmmo_data", true);

                    // Get the player's power level
                    int powerLevel = (int) mcMMOPlayer.getClass().getMethod("getPowerLevel").invoke(mcMMOPlayer);
                    skillsResponse.addProperty("power_level", powerLevel);

                    // Get all skills
                    List<String> allSkills = (List<String>) skillAPIClass.getMethod("getSkills").invoke(null);

                    // Create skills object for leveled skills
                    JsonObject leveledSkills = new JsonObject();

                    // Add each skill with level > 0
                    for (String skillName : allSkills) {
                        Object skillType = Enum.valueOf(
                                (Class<Enum>) primarySkillTypeClass,
                                skillName);

                        int skillLevel = (int) mcMMOPlayer.getClass()
                                .getMethod("getSkillLevel", primarySkillTypeClass)
                                .invoke(mcMMOPlayer, skillType);

                        // Only include skills with levels
                        if (skillLevel > 0) {
                            JsonObject skillObject = new JsonObject();

                            skillObject.addProperty("level", skillLevel);

                            int skillXp = (int) mcMMOPlayer.getClass()
                                    .getMethod("getSkillXpLevel", primarySkillTypeClass)
                                    .invoke(mcMMOPlayer, skillType);

                            int xpToNextLevel = (int) mcMMOPlayer.getClass()
                                    .getMethod("getXpToLevel", primarySkillTypeClass)
                                    .invoke(mcMMOPlayer, skillType);

                            skillObject.addProperty("xp", skillXp);
                            skillObject.addProperty("xp_to_next_level", xpToNextLevel);

                            leveledSkills.add(skillName, skillObject);
                        }
                    }

                    skillsResponse.add("skills", leveledSkills);

                    // Get skill categories
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

                    skillsResponse.add("categories", categoriesObject);

                } catch (Exception e) {
                    skillsResponse.addProperty("has_mcmmo_data", false);
                    skillsResponse.addProperty("error", "Failed to retrieve mcMMO data: " + e.getMessage());
                }

            } catch (BadRequestResponse e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestResponse("Error retrieving player: " + e.getMessage());
            }

            ctx.json(skillsResponse.toString());
        });
    }

    private void loadTownsEndpoint() {
        TownsListEndpoint tle = new TownsListEndpoint();
        javalin.get("/towns", ctx -> ctx.json(tle.lookup()));

        TownsEndpoint townsEndpoint = new TownsEndpoint();
        javalin.post("/towns", ctx -> {
            ctx.json(townsEndpoint.lookup(parseBody(ctx.body())));
        });
    }

    private void loadNationsEndpoint() {
        NationsListEndpoint nle = new NationsListEndpoint();
        javalin.get("/nations", ctx -> ctx.json(nle.lookup()));

        NationsEndpoint nationsEndpoint = new NationsEndpoint();
        javalin.post("/nations", ctx -> {
            ctx.json(nationsEndpoint.lookup(parseBody(ctx.body())));
        });
    }
}