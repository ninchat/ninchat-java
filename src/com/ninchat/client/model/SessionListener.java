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


import com.ninchat.client.transport.events.Error;

/**
 * @author Kari Lavikka
 */
public abstract class SessionListener {
	public void onChannelCreated(Session session, Channel channel) { }
	public void onChannelDestroyed(Session session, Channel channel) { }

	public void onDialogueCreated(Session session, Dialogue dialogue) { }
	public void onDialogueDestroyed(Session session, Dialogue dialogue) { }

	public void onSessionEstablished(Session session) { }
	public void onSessionEnded(Session session) { } // TODO: Reason

	public void onAudienceQueueCreated(Session session, AudienceQueue audienceQueue) { }
	public void onAudienceQueueDestroyed(Session session, AudienceQueue audienceQueue) { }
	public void onAudienceQueueUpdated(Session session, AudienceQueue audienceQueue) { }

	/**
	 * After session has been established, some attributes like realms and dialogue peers have to be loaded before
	 * they can be shown in user interface. Session does this automatically after it has been established. This
	 * method is called when loading is complete.
	 *
	 * @param session
	 */
	public void onAttributesLoaded(Session session) { }

	public void onUserUpdated(Session session, User user) { }

	public void onError(Session session, Error error) { }
}
