package com.ninchat.client.transport.payloads;

import com.google.gson.annotations.SerializedName;
import com.ninchat.client.transport.attributes.ChannelAttrs;
import com.ninchat.client.transport.attributes.RealmAttrs;

/**
 * You were invited to a channel and optionally to its realm. user_id is the invitor.
 *
 * @author Kari
 */
public class NinchatInfoAccessMessage extends NinchatInfoMessage {
	public static final String MESSAGE_TYPE = "ninchat.com/info/access";

	@SerializedName("user_id")
	private String userId;

	@SerializedName("access_key")
	private String accessKey;

	@SerializedName("channel_id")
	private String channelId;

	@SerializedName("channel_attrs")
	private ChannelAttrs channelAttrs;

	@SerializedName("realm_id")
	private String realmId;

	@SerializedName("realm_attrs")
	private RealmAttrs realmAttrs;

	@SerializedName("realm_member")
	private boolean realmMember;


	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	public String getUserId() {
		return userId;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public String getChannelId() {
		return channelId;
	}

	public ChannelAttrs getChannelAttrs() {
		return channelAttrs;
	}

	public String getRealmId() {
		return realmId;
	}

	public RealmAttrs getRealmAttrs() {
		return realmAttrs;
	}

	public boolean isRealmMember() {
		return realmMember;
	}
}
