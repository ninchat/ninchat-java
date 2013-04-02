/*
 * Copyright (c) 2012-2013, Somia Reality Oy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.ninchat.client.model;

import com.ninchat.client.transport.Payload;
import com.ninchat.client.transport.events.MessageReceived;
import com.ninchat.client.transport.payloads.NinchatInfoMessage;
import com.ninchat.client.transport.payloads.NinchatLinkMessage;
import com.ninchat.client.transport.payloads.NinchatTextMessage;

import java.util.Date;

/**
 * @author Kari Lavikka
 */
public class Message implements Comparable {
	protected final String id;
	protected Date time;
	protected String type;
	protected String userId;
	protected String userName;
	protected float ttl;
	protected boolean fold;

	private final Payload payload;

	@Deprecated
	Message(String id, Payload payload) {
		this.id = id;
		this.payload = payload;
	}

	Message(String id) {
		this.id = id;
		this.payload = null;
	}

	Message(MessageReceived event) {
		this.id = event.getMessageId();
		time = new Date(event.getMessageTime() * 1000);
		userId = event.getMessageUserId();
		userName = event.getMessageUserName();

		if (event.getPayloadCount() > 0) {
			payload = event.getPayloads()[0];
		} else {
			payload = null;
		}
	}

	public String getId() {
		return id;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public float getTtl() {
		return ttl;
	}

	public void setTtl(float ttl) {
		this.ttl = ttl;
	}

	public boolean isFold() {
		return fold;
	}

	public void setFold(boolean fold) {
		this.fold = fold;
	}

	@Override
	public int compareTo(Object o) {
		return id.compareTo(((Message)o).id);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Message && id.equals(((Message)obj).id);
	}

	public String getText() {
		// TODO: Implement getText or something similar in all subclasses
		if (payload instanceof NinchatTextMessage) {
			return ((NinchatTextMessage)payload).getText();

		} else if (payload instanceof NinchatInfoMessage) {
			return ((NinchatInfoMessage)payload).getInfo();

		} else if (payload instanceof NinchatLinkMessage) {
			return ((NinchatLinkMessage)payload).getUrl();

		} else {
			return "-";
		}
	}

	/**
	 * Returns true if payload matches some of the given classes
	 *
	 * @param classes
	 * @return
	 */
	public boolean payloadMatches(Class<? extends Payload>... classes) {
		if (payload == null) return false;
		for (Class<? extends Payload> clazz : classes) {
			if (clazz.isInstance(payload)) return true;
		}
		return false;
	}

	public Payload getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "" + time + " <" + userId + "/" + userName + "> " + getText();
	}
}
