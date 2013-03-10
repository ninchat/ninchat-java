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

/**
 * @author Kari Lavikka
 */
public interface ActivityStatusListener {

	/**
	 * Called when a new message has been received and conversation had no unread or highlighted messages.
	 * User's own messages do not cause an event.
	 *
	 * @param conversation Conversation where event occured
	 * @param message Message that caused the status change
	 */
	public void onUnread(Conversation conversation, Message message);

	/**
	 * Called when a new message that contains highlightable tokens has been received and conversation had no
	 * highlighted messages. User's own messages do not cause an event.
	 *
	 * @param conversation Conversation where event occured
	 * @param message Message that caused the status change
	 */
	public void onHighlight(Conversation conversation, Message message);

	/**
	 * Called after conversation's unread messages have been marked read. Conversation can now trigger new onUnread
	 * or onHighligh event.
	 *
	 * @param conversation Conversation where event occured
	 */
	public void onNone(Conversation conversation);
}
