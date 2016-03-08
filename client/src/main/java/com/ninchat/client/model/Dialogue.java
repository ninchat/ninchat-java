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

import com.google.gson.JsonObject;
import com.ninchat.client.transport.actions.UpdateDialogue;

import java.util.SortedSet;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public class Dialogue extends Conversation {
	private final static Logger logger = Logger.getLogger(Dialogue.class.getName());

	private User peer;

	/** An optional audience queue that initiated this dialogue */
	private AudienceQueue audienceQueue;

	/** An optional metadata */
	private JsonObject audienceMetadata;

	public Dialogue(Session session, String id) {
		super(session, id, new WrappedId(id));

		if (session != null) {
			peer = session.getOrCreateUser(id);
		}
	}

	public SortedSet<Message> getMessages() {
		return messages;
	}

	public String getName() {
		String peerName = peer != null ? peer.getName() : null;

		if (peerName == null) {
			peerName = id;
		}

		if (audienceQueue != null) {
			peerName = peerName + " (" + audienceQueue.getName() + ")";
		}

		return peerName;
	}

	public User getPeer() {
		return peer;
	}

	public AudienceQueue getAudienceQueue() {
		return audienceQueue;
	}

	void setAudienceQueue(AudienceQueue audienceQueue) {
		this.audienceQueue = audienceQueue;
	}

	public JsonObject getAudienceMetadata() {
		return audienceMetadata;
	}

	public void setAudienceMetadata(JsonObject audienceMetadata) {
		this.audienceMetadata = audienceMetadata;
	}

	@Override
	public void leave() {
		UpdateDialogue updateDialogue = new UpdateDialogue();
		updateDialogue.setUserId(getId());
		updateDialogue.setDialogueStatus("hidden");

		session.getTransport().enqueue(updateDialogue);
	}


	public static class WrappedId extends Conversation.WrappedId {
		public WrappedId(String id) {
			super(id);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof WrappedId && ((WrappedId)obj).id.equals(id));
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public String toString() {
			return "/p/" + id;
		}
	}
}
