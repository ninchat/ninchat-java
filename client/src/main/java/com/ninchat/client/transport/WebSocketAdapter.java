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

import java.net.URI;
import java.util.Map;

/**
 * @author Kari Lavikka
 */
public abstract class WebSocketAdapter {
	protected Map<String, String> extraHeaders;
	protected URI uri;
	private WebSocketTransport webSocketTransport;

	public final void setWebSocketTransport(WebSocketTransport webSocketTransport) {
		this.webSocketTransport = webSocketTransport;
	}

	public void setURI(URI uri) {
		this.uri = uri;
	}

	public Map<String, String> getExtraHeaders() {
		return extraHeaders;
	}

	public void setExtraHeaders(Map<String, String> extraHeaders) {
		this.extraHeaders = extraHeaders;
	}

	public void connect() throws WebSocketAdapterException {
		connect(0);
	}

	public abstract void connect(int timeout) throws WebSocketAdapterException;
	public abstract void send(Object message) throws WebSocketAdapterException;
	public abstract void disconnect() throws WebSocketAdapterException;

	protected void onOpen() {
		webSocketTransport.onOpen();
	}

	protected void onClose(String reason) {
		webSocketTransport.onClose(reason);
	}

	protected void onMessage(Object message) {
		webSocketTransport.onMessage(message);
	}
}
