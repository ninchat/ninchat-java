package com.ninchat.client.transport.parameters;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * @author Kari
 */
public class AudienceMetadata {
	private JsonObject jsonObject;

	public AudienceMetadata(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public JsonObject getJsonObject() {
		return jsonObject;
	}

	public static class AudienceMetadataTypeAdapter implements JsonDeserializer<AudienceMetadata> {
		@Override
		public AudienceMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new AudienceMetadata(json.getAsJsonObject());
		}
	}
}
