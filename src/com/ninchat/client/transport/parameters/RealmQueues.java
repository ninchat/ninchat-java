package com.ninchat.client.transport.parameters;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * @author Kari
 */
public class RealmQueues extends HashMap<String, RealmQueues.Parameters> {
	public static class Parameters {
		@SerializedName("queue_attrs")
		private QueueAttrs queueAttrs;

		@SerializedName("queue_position")
		private int queuePosition;

		public QueueAttrs getQueueAttrs() {
			return queueAttrs;
		}

		public int getQueuePosition() {
			return queuePosition;
		}
	}
}
