package com.ninchat.client.transport.parameters;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * @author Kari
 */
public class QueueAttrs {
	@SerializedName("name")
	private String name;

	@SerializedName("length")
	private Integer length;

	@SerializedName("schedule")
	private Object schedule;

	@SerializedName("closed")
	private boolean closed;

	@SerializedName("suspended")
	private boolean suspended;

	// TODO: Schedule


	public void setName(String name) {
		this.name = name;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public String getName() {
		return name;
	}

	public Integer getLength() {
		return length;
	}

	public boolean isClosed() {
		return closed || suspended;
	}
}
