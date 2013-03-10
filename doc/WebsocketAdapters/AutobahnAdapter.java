package com.ninchat.client.android;

import com.ninchat.client.transport.WebSocketAdapter;
import com.ninchat.client.transport.WebSocketAdapterException;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketHandler;

/**
 *
 */
public class AutobahnAdapter extends WebSocketAdapter {
	private final WebSocketConnection webSocket = new WebSocketConnection();

	@Override
	public void connect() throws WebSocketAdapterException {
		try {
			webSocket.connect(uri.toString(), new WebSocketHandler() {
				@Override
				public void onBinaryMessage(byte[] payload) {
					AutobahnAdapter.this.onMessage(payload);
				}

				@Override
				public void onRawTextMessage(byte[] payload) {
					AutobahnAdapter.this.onMessage(payload);
				}

				@Override
				public void onTextMessage(String payload) {
					AutobahnAdapter.this.onMessage(payload);
				}

				@Override
				public void onClose(int code, String reason) {
					AutobahnAdapter.this.onClose(reason);
				}

				@Override
				public void onOpen() {
					AutobahnAdapter.this.onOpen();
				}
			});
		} catch (Exception e) {
			throw new WebSocketAdapterException(e);
		}
	}

	@Override
	public void send(Object message) throws WebSocketAdapterException {
		if (message instanceof String) {
			webSocket.sendTextMessage((String)message);
		} else if (message instanceof byte[]) {
			webSocket.sendBinaryMessage((byte[])message);
		} else {
			throw new WebSocketAdapterException("Invalid message type. Only String and byte[] are allowed.");
		}
	}

	@Override
	public void disconnect() throws WebSocketAdapterException {
		webSocket.disconnect();
	}
}
