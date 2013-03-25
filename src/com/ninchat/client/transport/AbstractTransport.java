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

import com.ninchat.client.transport.actions.Ping;
import com.ninchat.client.transport.events.Error;
import com.ninchat.client.transport.events.HistoryResults;
import com.ninchat.client.transport.events.MessageReceived;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public abstract class AbstractTransport {
	private final static Logger logger = Logger.getLogger(AbstractTransport.class.getName());

	protected final NavigableSet<Action> queue = new TreeSet<Action>();

	protected final AtomicLong lastAcknowledgedActionTimestamp = new AtomicLong(Long.MIN_VALUE);
	protected final AtomicLong lastSentActionTimestamp = new AtomicLong(Long.MIN_VALUE);

	protected Action lastSentAction;

	/** Timer for timeouting AckListeners */
	protected Timer timeoutTimer;


	public enum Status {
		CLOSED,
		OPENING,
		OPENED,
		/** Transport is being terminated because session has ended or is ending */
		TERMINATING
	}

	/** Last sent action id */
	protected final AtomicLong actionId = new AtomicLong();

	/** Last received event id */
	protected final AtomicLong eventId = new AtomicLong();

	protected Status status = Status.CLOSED;
	protected final Object statusHook = new Object();

	protected final Map<Class<? extends Event>, Set<TransportEventListener<? extends Event>>> eventListeners =
			new HashMap<Class<? extends Event>, Set<TransportEventListener<? extends Event>>>();

	protected final Set<TransportStatusListener> transportStatusListeners = new CopyOnWriteArraySet<TransportStatusListener>();

	protected String sessionId;

	protected boolean autoReconnect = true;

	protected String host = "api.ninchat.com";


	/**
	 * Returns the number of unset actions
	 *
	 * @return
	 */
	public long getQueueSize() {
		synchronized (queue) {
			if (lastSentAction != null) {
				// Return number of unsent messages
				return queue.tailSet(lastSentAction).size() - 1;

			} else {
				// Assume that all messages are unsent
				return queue.size();
			}
		}
	}


	/**
	 * Returns the number of unsent actions that are instances of the given class
	 */
	public long unsentActionsInQueue(Class<? extends Action> actionClass) {
		synchronized (queue) {
			SortedSet<Action> q;

			if (lastSentAction != null) {
				// Return number of unsent messages
				q = queue.tailSet(lastSentAction);

			} else {
				// Assume that all messages are unsent
				q = queue;
			}

			long count = 0;
			for (Action a : q) {
				if (actionClass.isInstance(a)) {
					count++;
				}
			}

			return count;
		}
	}

	/**
	 * Assings an action id and queues the action for sending.
	 * <p>
	 *     <code>close_session</code> action has a special handling. Transport is closed automatically after
	 *     it has been transmitted.
	 * </p>
	 *
	 * @param action action to queue
	 * @returns ActionId an unique action id
	 */
	public Long enqueue(Action action) {
		if (!action.verify()) {
			throw new IllegalArgumentException("Action validation failed! Probably some mandatory properties are missing.");
		}

		Long ai = actionId.getAndIncrement();
		action.setId(ai);

		Long lEventId = eventId.get();
		if (lEventId > Long.MIN_VALUE) {
			action.setEventId(lEventId); // TODO: Behave well and do not send same id multiple times
		}

		action.registerTimeoutTask(timeoutTimer);

		synchronized (queue) {
			boolean added = queue.add(action);
			assert added;

			queue.notifyAll();
		}

		logger.fine("Enqueued action: " + action);

		return action.isExpectActionId() ? ai : null;
	}

	public void addEventListener(Class<? extends Event> eventClass, TransportEventListener<? extends Event> eventListener) {
		Set<TransportEventListener<? extends Event>> listeners = eventListeners.get(eventClass);
		if (listeners == null) {
			listeners = new CopyOnWriteArraySet<TransportEventListener<? extends Event>>();
			eventListeners.put(eventClass, listeners);
		}

		if (listeners.add(eventListener)) {
			logger.fine("Added event listener " + eventListener.getClass().getName() + " for " + eventClass.getName());
		}
	}

	public void removeEventListener(Class<? extends Event> eventClass, TransportEventListener<? extends Event> eventListener) {
		Set<TransportEventListener<? extends Event>> listeners = eventListeners.get(eventClass);
		if (listeners != null) {
			if (listeners.remove(eventListener)) {
				logger.fine("Removed event listener " + eventListener.getClass().getName() + " from " + eventClass.getName());
			}
		}
	}

	public void addTransportStatusListener(TransportStatusListener transportStatusListener) {
		if (transportStatusListeners.add(transportStatusListener)) {
			logger.fine("Added transport status listener: " + transportStatusListener.getClass().getName());
		}
	}

	public void removeTransportStatusListener(TransportStatusListener transportStatusListener) {
		if (transportStatusListeners.remove(transportStatusListener)) {
			logger.fine("Removed transport status listener: " + transportStatusListener.getClass().getName());
		}
	}

	public boolean isConnected() {
		return status == Status.OPENED;
	}


	/**
	 * Terminates connection and clears queues. Practically prepares this transport for new session.
	 */
	public void terminate() {
		setStatus(Status.CLOSED);

		synchronized (queue) {
			queue.clear();
		}

		init();
	}

	/**
	 * Prepares this transport for new session. Effectively clears all queues and initializes new worker threads
	 */
	protected void init() {
		if (status != Status.CLOSED) {
			throw new IllegalStateException("Can not initialize an opened or opening transport!");
		}

		if (timeoutTimer != null) {
			timeoutTimer.cancel();
		}
		timeoutTimer = new Timer();

		rewindQueue();
	}


	/**
	 * Sets sessionId for resume_session action
	 *
	 * @param sessionId session id
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	protected void setStatus(Status status) {
		if (status != this.status) {
			this.status = status;

			logger.info("Connection status: " + status);

			if (status == Status.CLOSED && autoReconnect && sessionId != null && getQueueSize() == 0) {
				ping(); // Add something to queue to trigger reconnect attempt. TODO: This is not pretty
			}

			synchronized (statusHook) {
				statusHook.notifyAll();
			}

			for (TransportStatusListener l : transportStatusListeners) {
				switch (status) {
					case OPENED: l.onOpen(this); break;
					case OPENING: l.onOpening(this); break;
					case CLOSED: l.onClose(this); break;
				}
			}
		}
	}


	@SuppressWarnings("unchecked")
	protected void onCompleteEvent(Event event) {
		if (logger.isLoggable(Level.FINER)) logger.finer("Complete event: " + event);

		if (event instanceof Error) {
			if ("session_not_found".equals(((Error) event).getErrorType())) {
				// Transport sent resume_session but session was not found. Probably server has invalidated it.

				if (logger.isLoggable(Level.INFO)) logger.info("Server said \"session_not_found\". Terminating transport.");

				terminate();

				for (TransportStatusListener l : transportStatusListeners) {
					l.onInvalidSession(this);
				}
			}
		}

		Long ei = event.getId();
		if (ei != null) {
			assert ei >= eventId.get();

			eventId.set(ei);
		}

		boolean eventHandled = false;

		Set<TransportEventListener<? extends Event>> listeners = eventListeners.get(event.getClass());
		if (listeners != null) {
			for (TransportEventListener listener : listeners) {
				eventHandled = true;
				if (logger.isLoggable(Level.FINER)) logger.finer("Calling " + listener.getClass().getName() + " for event: " + event);

				listener.onEvent(event);
			}
		}

		if (!eventHandled) {
			if (logger.isLoggable(Level.FINE)) logger.fine("No handler for event: " + event);
		}
	}

	/**
	 * Removes an action from queue by using a response event
	 *
	 * @return removed action
	 */
	protected Action removeActionFromQueue(Event event) {
		if (event.getActionId() == null) {
			return null; // No-op
		}

		// Special case. We receive multiple response events but only last one should trigger an acknowledgement
		if (!isLastInBatch(event)) return null;

		Action dummy = new DummyAction(event.getActionId());
		Action action;

		boolean existed;

		synchronized (queue) {
			action = queue.ceiling(dummy);
			existed = queue.remove(dummy);
		}

		return existed ? action : null;
	}

	/**
	 * History request causes multiple events with same action_id.
	 *
	 * @param event true if event was last in batch
	 */
	private boolean isLastInBatch(Event event) {
		// It would be nice if this behaviour was more generalized in json protocol. Something like "remaining_batch_length"
		return !((
				event instanceof MessageReceived &&
						((MessageReceived)event).getHistoryLength() != null &&
						((MessageReceived)event).getHistoryLength() > 0
		) || (
				event instanceof HistoryResults &&
						((HistoryResults)event).getHistoryLength() != null &&
						((HistoryResults)event).getHistoryLength() > 0)
		);
	}

	protected void acknowledge(Action action, Event event) {
		assert action != null && event != null;
		assert action.getId() != null;
		assert action.getId().equals(event.getActionId());

		lastAcknowledgedActionTimestamp.set(System.currentTimeMillis());
		logger.finer("Removed acknowledged action #" + actionId + " from queue");

		// TODO: Only on last response (if there are multiple with same event id)

		action.cancelTimeoutTask();
		AckListener ackListener = action.getAckListener();
		if (ackListener != null) {
			if (event instanceof Error) {
				ackListener.onError(action, (Error)event);
			} else {
				ackListener.onAcknowledge(action, event);
			}

			action.setAckListener(null); // Allow GC.
		}
	}

	/**
	 * Rewinds queue for new connection. Unacknowledged actions will be sent again when session is resumed.
	 */
	protected void rewindQueue() {
		long now = System.currentTimeMillis();

		lastAcknowledgedActionTimestamp.set(now);
		lastSentActionTimestamp.set(now);

		lastSentAction = null;
		synchronized (queue) {
			// TODO: This is a kludge. Have to rethink this...
			for (Action a : queue) {
				a.sent = Long.MIN_VALUE;
			}
		}
	}

	public void ping() {
		enqueue(new Ping());
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		// TODO: validate
		this.host = host;
	}

	/**
	 * Dummy class for NavigableSet navigation
	 */
	private class DummyAction extends Action {
		private DummyAction(long id) {
			setId(id);
		}

		@Override public boolean isExpectActionId() { return true; }
		@Override public boolean verify() { return true; }
		@Override public String getActionName() { return null; }
	}
}
