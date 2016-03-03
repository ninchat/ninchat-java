package com.ninchat.client.android.utils;

import com.ninchat.client.transport.WebSocketAdapter;
import com.ninchat.client.transport.WebSocketAdapterException;
import de.roderick.weberknecht.*;

import java.util.logging.Logger;


public class WeberknechtAdapter extends WebSocketAdapter {
	private final static Logger logger = Logger.getLogger(WeberknechtAdapter.class.getName());

	private WebSocket websocket;

	public WeberknechtAdapter() {

	}

	@Override
	public void connect() throws WebSocketAdapterException {
		logger.fine("connect()");

		try {
			websocket = new WebSocketConnection(uri);

			// Register TransportEvent Handlers
			websocket.setEventHandler(new WebSocketEventHandler() {
				@Override
				public void onOpen() {
					logger.fine("onOpen()");
					WeberknechtAdapter.this.onOpen();
				}

				@Override
				public void onMessage(WebSocketMessage webSocketMessage) {
					WeberknechtAdapter.this.onMessage(webSocketMessage.getText());
				}

				@Override
				public void onClose() {
					logger.fine("onClose()");
					WeberknechtAdapter.this.onClose(null);
				}
			});

			websocket.connect();

		} catch (WebSocketException e) {
			throw new WebSocketAdapterException(e);
		}
	}

	@Override
	public void send(Object message) throws WebSocketAdapterException {
		try {
			websocket.send((String)message);

		} catch (Exception e) {
			throw new WebSocketAdapterException(e);
		}
	}

	@Override
	public void disconnect() throws WebSocketAdapterException {
		logger.fine("disconnect()");

		try {
			websocket.close();

		} catch (Exception e) {
			throw new WebSocketAdapterException(e);
		}
	}
}
