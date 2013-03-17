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
 * <p>Activity status listener is called when something notable happens in a conversation. There are three activity
 * states and three corresponding listener methods:</p>
 *
 * <dl>
 *     <dt>UNREAD</dt>
 *     <dd>One or more messages have been received</dd>
 *     <dt>HIGHLIGH</dt>
 *     <dd>On or more messages with highlight tokes have been received</dd>
 *     <dt>NONE</dt>
 *     <dd>All unread and highlighted messages have been marked as read</dd>
 * </dl>
 *
 * <p>Listener is called only when status changes, i.e., when first new message has been received.</p>
 *
 * @author Kari Lavikka
 */
public interface ActivityStatusListener {

	/**
	 * Called when a new message has been received and conversation had no earlier unread or highlighted messages.
	 * User's own messages do not cause an event.
	 *
	 * @param conversation Conversation where event occured
	 * @param message Message that caused the status change. Null if it was a history message (written before session was established).
	 */
	public void onUnread(Conversation conversation, Message message);

	/**
	 * Called when a new message that contains highlightable tokens has been received and conversation had no earlier
	 * highlighted messages. User's own messages do not cause an event.
	 *
	 * @param conversation Conversation where event occured
	 * @param message Message that caused the status change. Null if it was a history message (written before session was established).
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
