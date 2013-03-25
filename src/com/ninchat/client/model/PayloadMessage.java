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

/**
 * @author Kari Lavikka
 */
public class PayloadMessage extends Message {
	private final Payload payload;

	PayloadMessage(String id, Payload payload) {
		super(id);
		this.payload = payload;
	}

	PayloadMessage(MessageReceived event) {
		super(event);
		payload = event.getPayloads()[0];
	}

	public String getText() {
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

	public Payload getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "" + time + " <" + userId + "/" + userName + "> " + getText();
	}
}