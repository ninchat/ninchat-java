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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.ninchat.client.transport.actions.CloseSession;
import com.ninchat.client.transport.actions.ResumeSession;
import com.ninchat.client.transport.events.MessageReceived;
import com.ninchat.client.transport.payloads.MessagePayload;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public class WebSocketTransport extends AbstractTransport {
	private final static Logger logger = Logger.getLogger(WebSocketTransport.class.getName());

	private WebSocketAdapter webSocketAdapter;
	private int payloadFramesLeft;
	private Event currentEvent;

	private static final long TIMEOUT_ACTION = 20 * 1000; // TODO: Configurable
	private static final long TIMEOUT_CHECK_LAST_EVENT = 5 * 1000; // TODO: Configurable
	private static final long WAIT_BEFORE_PING = 90 * 1000; // TODO: Configurable

	private volatile QueueHog queueHog;
	private volatile TimeoutMonitor timeoutMonitor;

	private final Gson gson = new Gson();

	/**
	 * TimeoutWatcher waits in this object
	 */
	private final Object messageSentToWebsocketHook = new Object();

	public WebSocketTransport() {
		init();
	}

	public void terminate() {
		setStatus(Status.TERMINATING);

		if (queueHog != null) {
			queueHog.interrupt();
			try {
				queueHog.join(10000); // Timeout just for sure. Shouldn't be needed. TODO: Remove.
			} catch (InterruptedException e) {
				logger.warning("Interrupted while waiting for thread to join.");
			}
			queueHog = null;
		}

		try {
			// TODO: Better approach would be to signal QueueHog about termination. It's just not properly implemented.
			// There's already a graceful shutdown handling for "close_session" and it actually calls this terminate method.
			webSocketAdapter.disconnect();

		} catch (WebSocketAdapterException e) {
			logger.log(Level.FINE, "Can not terminate", e);
		}

		super.terminate();
	}

	/**
	 * Prepares this transport for new session. Effectively clears all queues and initializes new worker threads
	 */
	protected void init() {
		super.init();

		payloadFramesLeft = 0;
		currentEvent = null;

		if (queueHog == null) {
			queueHog = new QueueHog();
		} else {
			logger.warning("init(): QueueHog is not null!");
		}
	}

	public void setWebSocketAdapter(WebSocketAdapter webSocketAdapter) {
		this.webSocketAdapter = webSocketAdapter;
		webSocketAdapter.setWebSocketTransport(this);

	}

	@Override
	public Long enqueue(Action action) {
		synchronized (queueHog) {
			if (!queueHog.isAlive()) {
				queueHog.start();
			}
		}

		return super.enqueue(action);
	}

	/**
	 * Connects to server. It may be synchronous or asynchronous - depending on WebSocket implementation
	 *
	 * @return true if no errors were encountered
	 */
	private boolean connect() {
		if (status != Status.CLOSED) {
			logger.fine("Trying to connect but status is not CLOSED. Ignoring.");
			return false;
		}

		try {
			setStatus(Status.OPENING);

			URI uri = new URI("wss://" + host + "/socket");
			logger.info("Connecting to " + uri);
			webSocketAdapter.setURI(uri);
			webSocketAdapter.connect();

			return true;

		} catch (Exception e) {
			logger.log(Level.WARNING, "Can not connect", e);
			setStatus(Status.CLOSED);
		}

		return false;
	}

	void onOpen() {
		toggleTimeoutMonitor(true);
		setStatus(Status.OPENED);
	}


	void onClose(String reason) {
		toggleTimeoutMonitor(false);
		setStatus(Status.CLOSED);
	}

	private class DummyEvent extends Event {
		@Override public boolean verify() { return false; }
		@Override public String getEventName() { return null; }
	}

	void onMessage(Object message) {
		String text = (String)message; // TODO: Support binary frames

		if (payloadFramesLeft > 0) {
			logger.finest("Receiving payload: " + text);

			if (currentEvent instanceof PayloadEvent) {
				PayloadEvent pe = (PayloadEvent)currentEvent;

				if (currentEvent instanceof MessageReceived) {
					Class <? extends MessagePayload> payloadClass = MessagePayload.messageClasses.get(((MessageReceived)currentEvent).getMessageType());

					try {
						pe.payloads[pe.payloads.length - payloadFramesLeft] = gson.fromJson(text, payloadClass);

					} catch (JsonSyntaxException e) {
						logger.log(Level.WARNING, "Can not parse JSON", e);
					}

				} else {
					logger.warning("Only message_received event supports payloads atm...");
				}

			} else {
				logger.warning("Receiving payloadFrame although we should not!?");
			}
			payloadFramesLeft--;

		} else {
			if (logger.isLoggable(Level.FINEST)) logger.finest("Receiving header: " + text);

			if (text == null || text.length() == 0 || text.charAt(0) != '{') {
				logger.finest("Empty frame!");
				return;
			}

			try {
				String eventName = null;

				assert payloadFramesLeft == 0;

				// First we have to view received object briefly to figure out a concrete event type and the number of expected payload frames
				JsonReader reader = new JsonReader(new StringReader(text));
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if ("event".equals(name)) {
						eventName = reader.nextString();
					} else if ("frames".equals(name)) {
						payloadFramesLeft = reader.nextInt();
					} else {
						reader.skipValue();
					}
				}

				if (eventName == null) {
					logger.warning("Received a header but it does not contain an event type: " + text + " ... ignoring it.");
					return;
				}

				Class<? extends Event> eventClass = EventClassRegistry.eventClasses.get(eventName);
				if (eventClass == null) {
					logger.warning("Can not find a concrete class for event: " + eventName + " ... ignoring it.");
					return;
				}

				currentEvent = gson.fromJson(text, eventClass);
				if (currentEvent instanceof PayloadEvent) {
					((PayloadEvent)currentEvent).payloads = new Payload[payloadFramesLeft];
				}

				// We really should not get into these exception handlers. There's a risk that we mess up payload
				// counters and transport state gets corrupted. TODO: Session should probably get terminated now...

			} catch (JsonSyntaxException e) {
				logger.log(Level.SEVERE, "Error while parsing websocket message: " + message, e);

			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error while parsing websocket message: " + message, e);
			}

		}

		if (payloadFramesLeft <= 0) {
			// First remove action from queue
			Action action = removeActionFromQueue(currentEvent);

			// Then call generic listeners that are bound to transport and model
			onCompleteEvent(currentEvent);

			// Finally call specific listener that is bound to individual event. Now model is already updated when
			// listener gets a notification.
			if (action != null && action.isExpectActionId()) {
				acknowledge(action, currentEvent);
			}
		}

	}


	private void timeout() {
		logger.info("Timeout! Closing connection...");
		try {
			webSocketAdapter.disconnect();
		} catch (WebSocketAdapterException e) {
			logger.log(Level.WARNING, "Can not disconnect", e);
		}
	}

	private void toggleTimeoutMonitor(boolean run) {
		if (run) {
			if (timeoutMonitor == null || !timeoutMonitor.isAlive()) {
				timeoutMonitor = new TimeoutMonitor();
				timeoutMonitor.start();
			}

		} else {
			if (timeoutMonitor != null) {
				timeoutMonitor.interrupt();
				timeoutMonitor = null;
			}
		}

	}


	private class TimeoutMonitor extends Thread {
		@Override
		public void run() {
			try {
				setName("TimeoutMonitor");
			} catch (SecurityException e) {
				logger.log(Level.WARNING, "Can not set thread name", e);
			}

			logger.info("TimeoutMonitor: started!");

			try {
				while (!isInterrupted()) {
					Action action = null;

					// Pick tail of the queue
					synchronized (queue) {
						if (!queue.isEmpty()) {
							action = queue.last();
						}
					}

					if (action != null && action.getSent() > Long.MIN_VALUE) {
						// Found an unacknowledged action. This logic is somewhat complicated because actions are
						// acknowledged when they are picked from queue for processing. Timeout may get triggered
						// if processing is too slow. This can be worked around by adding yet another queue, but
						// that would be overly complicated. Currently the problem is mitigated by checking the timestamp
						// of the previous acknowledged action. If it was just a while ago, we are probably busy
						// processing the event. An "inEventListener" variable could also be introduced.

						logger.finer("TimeoutMonitor: Found an unacknowledged action #" + action.getId() + " from queue.");

						long currentTime = System.currentTimeMillis();

						long timeLeft = action.getSent() + TIMEOUT_ACTION - currentTime;
						if (timeLeft < 0) {
							// Already beyond timeout
							logger.fine("TimeoutMonitor: Found a timed out action " + action + " which was sent " +
									(currentTime - action.getSent()) + " ms ago");

							long lastAck = currentTime - lastAcknowledgedActionTimestamp.get();
							if (currentTime - lastAcknowledgedActionTimestamp.get() < TIMEOUT_CHECK_LAST_EVENT) {
								long nap = TIMEOUT_CHECK_LAST_EVENT - lastAck;

								logger.fine("TimeoutMonitor: However, previous event was acknowledged just " + lastAck +
								" ms ago. Let's wait " + nap + " ms. Maybe we are just so busy handling response events.");
								sleep(nap);

							} else {
								timeout();
							}

						} else {
							logger.finer("TimeoutMonitor: Waiting " + timeLeft + "ms for timeout.");
							// Wait until timeout
							sleep(timeLeft);

							// And check if action is still unacknowledged
							boolean acknowledged;
							synchronized (queue) {
								acknowledged = !queue.contains(action);
							}

							if (!acknowledged) {
								// Still in queue. Check time left again because it might have been modified during sleep.
								timeLeft = action.getSent() + TIMEOUT_ACTION - currentTime;
								if (timeLeft < 0) {
									logger.fine("TimeoutMonitor: Action #" + action.getId() + " timed out while waiting for acknowledgement! " + action.toString());
									timeout();
								}

							} else {
								logger.finer("TimeoutMonitor: Action #" + action.getId() + " was acknowledged during wait.");
							}
						}

					} else {
						// There are no unacknowledged actions

						// TODO: Ping logic probably needs some polishment. Doesn't look that nice...

						long timeUntilPing = Math.max(lastSentActionTimestamp.get(), lastAcknowledgedActionTimestamp.get()) - System.currentTimeMillis() + WAIT_BEFORE_PING;

						timeUntilPing = Math.max(timeUntilPing, 5000); // Wait at least 5 sec. Kluns...

						logger.finest("TimeoutMonitor: Queue is empty. Waiting " + timeUntilPing + "ms, until a message has been sent to WebSocket or it is time to PING.");
						synchronized (messageSentToWebsocketHook) {
							messageSentToWebsocketHook.wait(timeUntilPing);
						}

						// Check whether wake up was for a ping or a new queued message
						timeUntilPing = Math.max(lastSentActionTimestamp.get(), lastAcknowledgedActionTimestamp.get()) - System.currentTimeMillis() + WAIT_BEFORE_PING;
						if (timeUntilPing < 0 && status == Status.OPENED && sessionId != null) {
							// Only ping if session is established
							ping();
						}
					}
				}

			} catch (InterruptedException e) {
				logger.fine("TimeoutMonitor: Thread interrupted");
			}

			logger.info("TimeoutMonitor: Thread terminates");
		}
	};

	private class QueueHog extends Thread {
		final long initialReconnectDelay = 1000;
		long reconnectDelay = initialReconnectDelay;

		@Override
		public void run() {
			try {
				setName("QueueHog");
			} catch (SecurityException e) {
				logger.log(Level.WARNING, "Can not set thread name", e);
			}

			logger.info("QueueHog: started!");

			try {
				pickFromQueue:

				while (!isInterrupted()) {
					Action action;

					// Wait for something to send
					synchronized (queue) {
						do {
							if (lastSentAction == null) {
								// Pick first if nothing has been sent before
								action = queue.isEmpty() ? null : queue.first();
							} else {
								// Or the next one
								action = queue.higher(lastSentAction);
							}

							if (action == null) {
								logger.fine("QueueHog: Got nothing from queue. Waiting for action.");

								queue.wait();
							}
						} while (action == null);
					}

					// Open connection if it is closed
					while (status != Status.OPENED) {
						if (status == Status.CLOSED) {
							logger.fine("QueueHog: calling connect()");
							connect();
						}

						synchronized (statusHook) {
							if (status == Status.OPENING) {
								logger.info("QueueHog: Waiting for opened connection");
								statusHook.wait(); // WebSocket onOpen callback wakes me up
							}
						}

						if (status == Status.CLOSED) {
							logger.fine("QueueHog: Connection attempt failed");
							// If connect failed ...

							logger.fine("QueueHog: Sleeping " + reconnectDelay + "ms before trying again");
							sleep(reconnectDelay);
							reconnectDelay *= 1.5;

						} else if (status == Status.OPENED) {
							if (sessionId != null) {
								logger.fine("QueueHog: Resuming session");
								// If connection was opened and session is is present

								try {
									Action r = new ResumeSession();
									r.setSessionId(sessionId);
									r.setEventId(eventId.get());
									JsonElement element = gson.toJsonTree(r);
									element.getAsJsonObject().addProperty("action", r.getActionName());

									String json = gson.toJson(element);
									logger.finer("QueueHog: sending resume_session to WebSocket: " + json);

									webSocketAdapter.send(json);

									// If resume_session fails, we get an error event with error type "session_not_found"

									reconnectDelay = initialReconnectDelay; // TODO: This should probably be set after successful session negotiation

									rewindQueue();

									continue pickFromQueue;

								} catch (Exception e) {
									logger.log(Level.WARNING, "Can't send resume_session", e);
									// TODO: Terminate session gracefully
								}

							} else {
								logger.fine("QueueHog: Got a connection");

								reconnectDelay = initialReconnectDelay; // TODO: This should probably be set after successful session negotiation
							}
						}
					}

					// Include action name and payload count
					JsonElement element = gson.toJsonTree(action);
					element.getAsJsonObject().addProperty("action", action.getActionName());
					if (action instanceof PayloadAction) {
						element.getAsJsonObject().addProperty("frames", ((PayloadAction)action).getPayloadCount());
					}
					String header = gson.toJson(element);

					boolean closingRequest = action instanceof CloseSession;
					if (closingRequest) {
						logger.fine("QueueHog: Sending close_session action. I'll quit after this action!");
					}

					try {
						logger.finer("QueueHog: sending header to WebSocket: " + header);

						webSocketAdapter.send(header);

						if (action instanceof PayloadAction) {
							Payload [] payloads = ((PayloadAction)action).getPayloads();

							if (payloads != null && payloads.length >= 1) {
								for (Payload payload : payloads) {
									String json = "{}";

									if (payload != null) {
										json = gson.toJson(payload);
									}

									logger.finer("QueueHog: sending payload to WebSocket: " + json);
									webSocketAdapter.send(json);
								}
							}
						}

						lastSentActionTimestamp.set(System.currentTimeMillis());

						if (action.isExpectActionId()) {
							action.flagSent();
							synchronized (messageSentToWebsocketHook) {
								messageSentToWebsocketHook.notifyAll();
							}

							synchronized (queue) {
								lastSentAction = action;
							}

						} else {
							// Actions without actionId must not be retransmitted or tracked by TimeoutMonitor
							synchronized (queue) {
								queue.remove(action);
							}
						}

					} catch (WebSocketAdapterException e) {
						logger.log(Level.WARNING, "Problem with WebSocket. I'll quit!", e);
						closingRequest = true;
					}

					if (closingRequest) {
						logger.fine("QueueHog: Interrupting QueueHog and terminating transport.");
						interrupt();
						terminate();
					}

				}

			} catch (InterruptedException e) {
				logger.fine("QueueHog: Thread interrupted");
			}

			logger.fine("QueueHog: Thread terminates");
		}
	}

}




