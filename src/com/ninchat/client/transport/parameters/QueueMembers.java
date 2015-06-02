package com.ninchat.client.transport.parameters;

import com.google.gson.annotations.SerializedName;
import com.ninchat.client.transport.attributes.ChannelMemberAttrs;
import com.ninchat.client.transport.attributes.UserAttrs;

import java.util.HashMap;

/**
 * @author Kari
 */
public class QueueMembers extends HashMap<String, ChannelMembers.Parameters> {

	public static class Parameters {
		@SerializedName("user_attrs")
		private UserAttrs userAttrs;

		@SerializedName("member_attrs")
		private ChannelMemberAttrs memberAttrs;

		public UserAttrs getUserAttrs() {
			return userAttrs;
		}

		public ChannelMemberAttrs getMemberAttrs() {
			return memberAttrs;
		}
	}
}
