package net.orbismc.omcapi.object.endpoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.orbismc.omcapi.OMCAPI;

public abstract class PostEndpoint<T> {

    public String lookup(JsonArray queryArray) {
        JsonArray jsonArray = new JsonArray();

        int numLoops = Math.min(OMCAPI.instance.getConfig().getInt("behaviour.max_lookup_size"), queryArray.size());
        for (int i = 0; i < numLoops; i++) {
            JsonElement element = queryArray.get(i);
            T object = getObjectOrNull(element);

            JsonElement innerObject;
            if (object == null) {
                innerObject = null;
            } else {
                innerObject = getJsonElement(object);
            }

            jsonArray.add(innerObject);
        }

        return jsonArray.toString();
    }

    public abstract T getObjectOrNull(JsonElement element);

    public abstract JsonElement getJsonElement(T object);
}
