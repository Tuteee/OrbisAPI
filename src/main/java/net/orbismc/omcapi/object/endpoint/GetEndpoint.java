package net.orbismc.omcapi.object.endpoint;

import com.google.gson.JsonElement;

public abstract class GetEndpoint {

    public abstract String lookup();

    public abstract JsonElement getJsonElement();
}
