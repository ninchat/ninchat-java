package com.ninchat.client.transport.parameters;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

import com.ninchat.client.transport.attributes.TagAttrs;

public class TagChildren extends HashMap<String, TagChildren.Child> {
	public static class Child {
		@SerializedName("tag_attrs")
		private TagAttrs tagAttrs;

		@SerializedName("tag_children")
		private TagChildren tagChildren;

		public TagAttrs getTagAttrs() {
			return tagAttrs;
		}

		public TagChildren getTagChildren() {
			return tagChildren;
		}
	}
}
