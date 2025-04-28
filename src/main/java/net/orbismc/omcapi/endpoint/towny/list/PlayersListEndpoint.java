package net.orbismc.omcapi.endpoint.towny.list;

import com.google.gson.JsonArray;
import com.palmergames.bukkit.towny.TownyAPI;
import net.orbismc.omcapi.object.endpoint.GetEndpoint;
import net.orbismc.omcapi.util.EndpointUtils;

public class PlayersListEndpoint extends GetEndpoint {

    @Override
    public String lookup() {
        return getJsonElement().toString();
    }

    @Override
    public JsonArray getJsonElement() {
        return EndpointUtils.getResidentArray(TownyAPI.getInstance().getResidents());
    }
}
