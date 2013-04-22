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

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public abstract class Action implements Comparable {
	private final static Logger logger = Logger.getLogger(Action.class.getName());

	transient long sent = Long.MIN_VALUE;

	@SerializedName("action_id")
	private Long id;

	/** Acknowledge last event the server has sent */
	@SerializedName("event_id")
	private Long eventId;

	/** WebSocket transport requires this for session resume. LongPoll transport requires this for every action */
	@SerializedName("session_id")
	private String sessionId;

	// ---

	public Long getId() {
		if (!isExpectActionId()) {
			throw new UnsupportedOperationException("" + this.getClass().getName() + " does not support action id!");
		}
		return id;
	}

	void setId(Long id) {
		if (this.id != null) {
			throw new IllegalStateException("Can not set new id. It is immutable!");
		}
		this.id = id;
	}

	void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	Long getEventId() {
		return eventId;
	}

	void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Whether or not the action will be replied with an event that contains the given action id
	 *
	 * @return true if actionId is supported
	 */
	public abstract boolean isExpectActionId();

	/**
	 * Checks that all mandatory fields are set. TODO: Rename to validate()
	 *
	 * @return true if all ok
	 */
	public abstract boolean verify(); // TODO: Could throw exception instead of returning false.

	/**
	 * Returns json event type for this Action
	 *
	 * @return type
	 */
	public abstract String getActionName();

	private transient volatile AckListener ackListener;

	private transient long ackListenerTimeout;

	private transient TimeoutTask timeoutTask;

	@Override
	public int compareTo(Object o) {
		Action bo = (Action)o;

		Long a = id;
		if (a == null) throw new IllegalStateException("Can not compare: no action id has been set for this action!");

		Long b = bo.id;
		if (b == null) throw new IllegalStateException("Can not compare: no action id has been set for given action!");

		return (int)(a - b);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Action)) return false;

		long a = getId();
		if (a == Long.MIN_VALUE) return false;

		long b = ((Action)o).getId();
		if (b == Long.MIN_VALUE) return false;

		return a == b;
	}

	@Override
	public int hashCode() {
		if (id == null) return 0;
		return (int)((long)id);
	}

	public void flagSent() {
		sent = System.currentTimeMillis();
	}

	/**
	 * Returns time when this action was sent to socket
	 *
	 * @return
	 */
	public long getSent() {
		return sent;
	}

	public AckListener getAckListener() {
		return ackListener;
	}

	public void setAckListener(AckListener ackListener, long timeout) {
		if (!isExpectActionId()) {
			throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support AckListeners!");
		}
		this.ackListener = ackListener;
		this.ackListenerTimeout = timeout;
	}

	public void setAckListener(AckListener ackListener) {
		setAckListener(ackListener, 0);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{" +
				(isExpectActionId() ? "id=" + id + ", ": "") +
				"eventId=" + eventId +
				(sent > Long.MIN_VALUE ? ", sent=" + new Date(sent) : "") +
				'}';
	}

	class TimeoutTask extends TimerTask {
		@Override
		public void run() {
			// Avoid race conditions with volatile variable and nulling it before executing the listener
			AckListener tmp = ackListener;
			ackListener = null;

			if (tmp != null) {
				logger.fine(this + " timed out. Calling listener: " + tmp.getClass().getName());
				try {
					tmp.onTimeout(Action.this);

				} catch (Exception e) {
					// Must caught all exception here. Otherwise Timer cancels all tasks and ceases to function.
					logger.log(Level.WARNING, "Listener threw an exception!", e);
				}
			}
		}
	}

	void registerTimeoutTask(Timer timer) {
		if (ackListener == null || ackListenerTimeout <= 0) {
			// No op
			return;
		}

		if (timeoutTask != null) {
			throw new IllegalStateException("TimeoutTask is already created!");
		}

		timeoutTask = new TimeoutTask();
		timer.schedule(timeoutTask, ackListenerTimeout);
	}

	void cancelTimeoutTask() {
		if (timeoutTask != null) {
			timeoutTask.cancel();
		}
	}
}
