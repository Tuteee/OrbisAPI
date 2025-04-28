package net.orbismc.omcapi.endpoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import me.casperge.realisticseasons.calendar.Date;
import net.orbismc.omcapi.object.endpoint.GetEndpoint;
import net.orbismc.omcapi.util.EndpointUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.time.LocalTime;
import me.casperge.realisticseasons.api.SeasonsAPI;
public class ServerEndpoint extends GetEndpoint {
    @Override
    public String lookup() {
        return getJsonElement().toString();
    }

    @Override
    public JsonObject getJsonElement() {
        JsonObject serverObject = new JsonObject();
        SeasonsAPI seasonsapi = SeasonsAPI.getInstance();
        TownyAPI townyAPI = TownyAPI.getInstance();

        World overworld = Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(Bukkit.getWorlds().get(0));

        serverObject.addProperty("version", Bukkit.getMinecraftVersion());
        serverObject.addProperty("moonPhase", overworld.getMoonPhase().toString());

        JsonObject timestampsObject = new JsonObject();
        timestampsObject.addProperty("newDayTime", TownySettings.getNewDayTime());
        timestampsObject.addProperty("serverTimeOfDay", LocalTime.now().toSecondOfDay());
        serverObject.add("timestamps", timestampsObject);

        JsonObject statusObject = new JsonObject();

        boolean isWinter = seasonsapi.getSeason(overworld).toString().equalsIgnoreCase("Winter");
        boolean isSnowing = isWinter && (overworld.hasStorm() || overworld.isThundering());

        statusObject.addProperty("hasStorm", overworld.hasStorm());
        statusObject.addProperty("isThundering", overworld.isThundering());
        statusObject.addProperty("isSnowing", isSnowing);

        serverObject.add("status", statusObject);

        JsonObject statsObject = new JsonObject();
        statsObject.addProperty("time", overworld.getTime());
        statsObject.addProperty("fullTime", overworld.getFullTime());
        statsObject.addProperty("maxPlayers", Bukkit.getMaxPlayers());
        statsObject.addProperty("numOnlinePlayers", Bukkit.getOnlinePlayers().size());
        statsObject.addProperty("numOnlineNomads", EndpointUtils.getNumOnlineNomads());
        statsObject.addProperty("numResidents", townyAPI.getResidents().size());
        statsObject.addProperty("numNomads", townyAPI.getResidentsWithoutTown().size());
        statsObject.addProperty("numTowns", townyAPI.getTowns().size());
        statsObject.addProperty("numTownBlocks", townyAPI.getTownBlocks().size());
        statsObject.addProperty("numNations", townyAPI.getNations().size());
        serverObject.add("stats", statsObject);


        JsonObject seasonsObject = new JsonObject();
        JsonArray worldSeasonsArray = new JsonArray();

        // Iterate through all worlds to capture season details
        for (World world : Bukkit.getWorlds()) {
            JsonObject worldSeasonObject = new JsonObject();
            worldSeasonObject.addProperty("worldName", world.getName());

            Date worldDate = seasonsapi.getDate(world);
            if (worldDate != null) {
                worldSeasonObject.addProperty("date", worldDate.toString());
                worldSeasonObject.addProperty("currentSeason", seasonsapi.getSeason(world).toString());
                worldSeasonObject.addProperty("dayOfWeek", seasonsapi.getDayOfWeek(world));
                worldSeasonObject.addProperty("monthName", seasonsapi.getCurrentMonthName(world));
                worldSeasonObject.addProperty("worldSeconds", seasonsapi.getSeconds(world));
                worldSeasonObject.addProperty("worldMinutes", seasonsapi.getMinutes(world));
                worldSeasonObject.addProperty("worldHours", seasonsapi.getHours(world));
                worldSeasonsArray.add(worldSeasonObject);
            }
        }

        seasonsObject.add("worlds", worldSeasonsArray);
        serverObject.add("seasons", seasonsObject);


        return serverObject;
    }
}