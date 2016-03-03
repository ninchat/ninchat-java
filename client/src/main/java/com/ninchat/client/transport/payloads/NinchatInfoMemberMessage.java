package com.ninchat.client.transport.payloads;

import com.google.gson.annotations.SerializedName;

/**
 * A channel member's silenced attribute changed.
 *
 * @author Kari
 */
public class NinchatInfoMemberMessage extends NinchatInfoMessage {
	public static final String MESSAGE_TYPE = "ninchat.com/info/member";


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
