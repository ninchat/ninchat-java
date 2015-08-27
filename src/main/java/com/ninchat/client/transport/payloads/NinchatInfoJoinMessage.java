package com.ninchat.client.transport.payloads;

import com.google.gson.annotations.SerializedName;

/**
 * A user joined the channel.
 *
 * @author Kari
 */
public class NinchatInfoJoinMessage extends NinchatInfoMessage {
	public static final String MESSAGE_TYPE = "ninchat.com/info/join";

	@SerializedName("user_id")
	private String userId;

	@SerializedName("user_name")
	private String userName;

	@SerializedName("member_silenced")
	private boolean memberSilenced;

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

	public boolean isMemberSilenced() {
		return memberSilenced;
	}
}
