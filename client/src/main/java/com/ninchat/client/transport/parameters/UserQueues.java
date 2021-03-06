package com.ninchat.client.transport.parameters;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * @author Kari
 */
public class UserQueues extends HashMap<String, UserQueues.Parameters> {
	public static class Parameters {
		@SerializedName("queue_attrs")
		private QueueAttrs queueAttrs;

		@SerializedName("realm_id")
		private String realmId;

		public QueueAttrs getQueueAttrs() {
			return queueAttrs;
		}

		public String getRealmId() {
			return realmId;
		}
	}
}
