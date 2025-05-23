package net.orbismc.omcapi.endpoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import io.javalin.http.BadRequestResponse;
import net.orbismc.omcapi.object.endpoint.PostEndpoint;
import net.orbismc.omcapi.object.nearby.DiscordContext;
import net.orbismc.omcapi.object.nearby.DiscordType;
import net.orbismc.omcapi.util.JSONUtil;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordEndpoint extends PostEndpoint<DiscordContext> {

    private final AccountLinkManager alm = DiscordSRV.getPlugin().getAccountLinkManager();

    @Override
    public DiscordContext getObjectOrNull(JsonElement element) {
        JsonObject jsonObject = JSONUtil.getJsonElementAsJsonObjectOrNull(element);
        if (jsonObject == null) throw new BadRequestResponse("Your query contains a value that is not a JSON object");

        JsonElement typeElement = jsonObject.get("type");
        JsonElement targetElement = jsonObject.get("target");
        if (typeElement == null || targetElement == null) throw new BadRequestResponse("Your JSON query is missing a type or target");

        String typeString = JSONUtil.getJsonElementAsStringOrNull(typeElement);
        String target = JSONUtil.getJsonElementAsStringOrNull(targetElement);
        if (typeString == null || target == null) throw new BadRequestResponse("Your JSON query has an invalid type or target");

        try {
            DiscordType type = DiscordType.valueOf(typeString.toUpperCase());
            return new DiscordContext(type, target);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Specified type is not valid");
        }
    }

    @Override
    public JsonElement getJsonElement(DiscordContext context) {
        DiscordType type = context.getType();
        String target = context.getTarget();

        JsonObject discordObject = new JsonObject();
        if (type == DiscordType.DISCORD) {
            Pattern pattern = Pattern.compile("^\\d{17,19}$");
            Matcher matcher = pattern.matcher(target);

            if (!matcher.find()) throw new BadRequestResponse(target + " is not a valid Discord ID");

            UUID uuid = alm.getUuid(target);
            discordObject.addProperty("id", target);
            discordObject.addProperty("uuid", uuid == null ? null : uuid.toString());
        } else if (type == DiscordType.MINECRAFT) {
            UUID uuid;
            try {
                uuid = UUID.fromString(target);
            } catch (IllegalArgumentException e) {
                throw new BadRequestResponse(target + " is not a valid Minecraft UUID");
            }

            discordObject.addProperty("id", alm.getDiscordId(uuid));
            discordObject.addProperty("uuid", uuid.toString());
        }

        return discordObject;
    }
}