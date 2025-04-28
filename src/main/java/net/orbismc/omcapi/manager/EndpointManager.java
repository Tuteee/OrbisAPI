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
import org.bukkit.configuration.file.FileConfiguration;

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