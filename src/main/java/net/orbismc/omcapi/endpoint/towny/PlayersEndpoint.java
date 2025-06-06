package net.orbismc.omcapi.endpoint.towny;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import io.javalin.http.BadRequestResponse;
import net.orbismc.omcapi.object.endpoint.PostEndpoint;
import net.orbismc.omcapi.util.EndpointUtils;
import net.orbismc.omcapi.util.JSONUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import java.util.List;
import java.util.UUID;

public class PlayersEndpoint extends PostEndpoint<Resident> {

    private static Economy economy = null;
    public PlayersEndpoint(Economy economy) {
        PlayersEndpoint.economy = economy;
    }

    @Override
    public Resident getObjectOrNull(JsonElement element) {
        String string = JSONUtil.getJsonElementAsStringOrNull(element);
        if (string == null) throw new BadRequestResponse("Your query contains a value that is not a string");

        Resident resident;
        try {
            resident = TownyAPI.getInstance().getResident(UUID.fromString(string));
        } catch (IllegalArgumentException e) {
            resident = TownyAPI.getInstance().getResident(string);
        }

        return resident;
    }

    @Override
    public JsonElement getJsonElement(Resident resident) {
        JsonObject playerObject = new JsonObject();

        playerObject.addProperty("name", resident.getName());
        playerObject.addProperty("uuid", resident.getUUID().toString());

        playerObject.addProperty("title", resident.getTitle().isEmpty() ? null : resident.getTitle());
        playerObject.addProperty("surname", resident.getSurname().isEmpty() ? null : resident.getSurname());
        playerObject.addProperty("formattedName", resident.getFormattedName());
        playerObject.addProperty("about", resident.getAbout().isEmpty() ? null : resident.getAbout());
        playerObject.add("town", EndpointUtils.getTownJsonObject(resident.getTownOrNull()));
        playerObject.add("nation", EndpointUtils.getNationJsonObject(resident.getNationOrNull()));

        JsonObject timestampsObject = new JsonObject();
        timestampsObject.addProperty("registered", resident.getRegistered());
        timestampsObject.addProperty("joinedTownAt", resident.hasTown() ? resident.getJoinedTownAt() : null);
        timestampsObject.addProperty("lastOnline", resident.getLastOnline() != 0 ? resident.getLastOnline() : null);
        playerObject.add("timestamps", timestampsObject);

        JsonObject statusObject = new JsonObject();
        statusObject.addProperty("isOnline", resident.isOnline());
        statusObject.addProperty("isNPC", resident.isNPC());
        statusObject.addProperty("isMayor", resident.isMayor());
        statusObject.addProperty("isKing", resident.isKing());
        statusObject.addProperty("hasTown", resident.hasTown());
        statusObject.addProperty("hasNation", resident.hasNation());
        playerObject.add("status", statusObject);

        JsonObject statsObject = new JsonObject();
        statsObject.addProperty("balance", resident.getPlayer() != null ? economy.getBalance(resident.getPlayer()) : economy.getBalance(Bukkit.getOfflinePlayer(resident.getUUID())));
        statsObject.addProperty("numFriends", resident.getFriends().size());
        playerObject.add("stats", statsObject);

        playerObject.add("perms", EndpointUtils.getPermsObject(resident.getPermissions()));

        JsonObject ranksObject = new JsonObject();
        ranksObject.add("townRanks", getRankArray(resident.getTownRanks()));
        ranksObject.add("nationRanks", getRankArray(resident.getNationRanks()));
        playerObject.add("ranks", ranksObject);

        playerObject.add("friends", EndpointUtils.getResidentArray(resident.getFriends()));

        return playerObject;
    }

    private JsonArray getRankArray(List<String> ranks) {
        JsonArray jsonArray = new JsonArray();

        for (String rank : ranks) {
            jsonArray.add(rank);
        }

        return jsonArray;
    }
}
