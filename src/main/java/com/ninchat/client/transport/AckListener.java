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

package com.ninchat.client.transport;

import com.ninchat.client.transport.events.Error;

/**
 * AckListener describes an interface for receiving an acknowledgement for Action.
 *
 * @author Kari Lavikka
 */
public interface AckListener {
	/**
	 * Called when server acknowledges the action with an Event
	 *
	 * @param action Action that was acknowledged
	 * @param response Event that acknowledged the Action
	 */
	public void onAcknowledge(Action action, Event response);

	/**
	 * Called when server acknowledges the action with an Error event
	 *
	 * @param action Action that was acknowledged
	 * @param response Error event that acknowledged the Action
	 */
	public void onError(Action action, Error response);

	/**
	 * Called if the action timed out. This is only called if a timeout was defined.
	 *
	 * @param action Action that timed out
	 */
	public void onTimeout(Action action);

	/**
	 * Called if action was removed from output queue before it was sent to WebSocket.
	 *
	 * @param action Action that was cancelled
	 */
	public void onCancel(Action action);
}
