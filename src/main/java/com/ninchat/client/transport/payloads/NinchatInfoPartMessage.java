package com.ninchat.client.transport.payloads;

import com.google.gson.annotations.SerializedName;

/**
 * @author Kari
 */
public class NinchatInfoPartMessage extends NinchatInfoMessage {
	public static final String MESSAGE_TYPE = "ninchat.com/info/part";

	@SerializedName("user_id")
	private String userId;

	@SerializedName("user_name")
	private String userName;

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	public String getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}
}
