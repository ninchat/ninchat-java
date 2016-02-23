package com.ninchat.client.transport.parameters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * @author Kari
 */
public class AudienceMetadata {

	public static class AudienceMetadataTypeAdapter implements JsonDeserializer<AudienceMetadata> {
		@Override
		public AudienceMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new AudienceMetadata();
		}
	}
}
