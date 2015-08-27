package com.ninchat.client.transport.payloads;

import com.google.gson.annotations.SerializedName;
import com.ninchat.client.transport.attributes.ChannelAttrs;

/**
 * Channel attributes changed.
 *
 * @author Kari
 */
public class NinchatInfoChannelMessage extends NinchatInfoMessage {
	public static final String MESSAGE_TYPE = "ninchat.com/info/channel";

	@SerializedName("channel_attrs_old")
	private ChannelAttrs channelAttrsOld;

	@SerializedName("channel_attrs_new")
	private ChannelAttrs channelAttrsNew;

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	public ChannelAttrs getChannelAttrsOld() {
		return channelAttrsOld;
	}

	public ChannelAttrs getChannelAttrsNew() {
		return channelAttrsNew;
	}
}
