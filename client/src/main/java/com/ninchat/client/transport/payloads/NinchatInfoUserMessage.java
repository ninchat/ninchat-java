package com.ninchat.client.transport.payloads;

import com.google.gson.annotations.SerializedName;

/**
 * @author Kari
 */
public class NinchatInfoUserMessage extends NinchatInfoMessage {
	public static final String MESSAGE_TYPE = "ninchat.com/info/user";

	@SerializedName("user_id")
	private String userId;

	@SerializedName("user_name")
	private String userName;

	@SerializedName("user_name_old")
	private String userNameOld;

	@SerializedName("user_deleted")
	private boolean userDeleted;

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

	public String getUserNameOld() {
		return userNameOld;
	}

	public boolean isUserDeleted() {
		return userDeleted;
	}
}
