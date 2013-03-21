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

import com.ninchat.client.transport.AckListener;
import com.ninchat.client.transport.actions.LoadHistory;
import com.ninchat.client.transport.actions.SendMessage;
import com.ninchat.client.transport.actions.UpdateSession;
import com.ninchat.client.transport.payloads.MessagePayload;
import com.ninchat.client.transport.payloads.NinchatTextMessage;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kari Lavikka
 */
public abstract class Conversation {
	private final static Logger logger = Logger.getLogger(Conversation.class.getName());

	protected final String id;
	protected final Session session; // TODO: Tää on tyhmä riippuvuus väärään suuntaan

	protected final Collection<ConversationListener> conversationListeners = new CopyOnWriteArraySet<ConversationListener>();

	protected final WrappedId wrappedId;
	// TODO: Limit history size
	protected final SortedSet<Message> messages = new TreeSet<Message>();

	public static final long DEFAULT_HISTORY_SIZE = 20;

	protected boolean historyLoaded = false;

	private String lastSeenMessageId = null;

	private final String SPLITTER = "[\\s\\p{Punct}]";

	public enum ActivityStatus {
		NONE(0),
		UNREAD(1),
		HIGHLIGHT(2);

		private int priority;
		ActivityStatus(int p) {
			priority = p;
		}

		public int getPriority() {
			return priority;
		}
	}

	ActivityStatus activityStatus = ActivityStatus.NONE;

	public Conversation(Session session, String id, WrappedId wrappedId) {
		this.id = id;
		this.session = session;
		this.wrappedId = wrappedId;
	}

	public SortedSet<Message> getMessages() {
		return messages;
	}

	/**
	 * Add a batch of messages. This should be used when message history is loaded. ActivityStatus stays intact
	 * because it is assumed that these messages have been written in the past.
	 *
	 * @param messages Messages to add
	 */
	void addMessages(List<Message> messages) {
		boolean hadNew = false;
		synchronized (this.messages) {
			hadNew = this.messages.addAll(messages);
		}

		if (!messages.isEmpty() && activityStatus == ActivityStatus.NONE) {
			// Make an assumption that user has seen all
			updateLastSeenMessageId(messages.get(messages.size() - 1).getId());
		}

		if (logger.isLoggable(Level.FINER))logger.finer("Messages added to chat " + getName());

		for (ConversationListener a : conversationListeners) {
			a.onMessages(this, messages);
		}
	}

	/**
	 * Add a single message. This should be used when a new message is received.
	 *
	 * @param message Message to add
	 */
	void addMessage(Message message) {
		boolean wasNew;

		synchronized (messages) {
			wasNew = messages.add(message);
		}

		if (logger.isLoggable(Level.FINER))logger.finer("Message added to chat " + getName());

		for (ConversationListener a : conversationListeners) {
			a.onMessage(this, message);
		}

		// If message was user's own (and sent from another client)...
		if (session.getUserId().equals(message.getUserId())) {
			// ... mark all messages automatically as seen.
			updateLastSeenMessageId(message.getId());

		} else if (isNewMessages()) { // A conversationListener may have marked it read already
			if (containsHighlightTokens(message)) {
				setActivityStatus(ActivityStatus.HIGHLIGHT);

			} else {
				setActivityStatus(ActivityStatus.UNREAD);
			}
		}
	}

	/**
	 * Updates last seen message id and calls activity status listeners if activity status changes.
	 */
	public void updateLastSeenMessageId() {
		Message latest;
		synchronized (messages) {
			latest = messages.isEmpty() ? null : messages.last();
		}
		if (latest != null) {
			updateLastSeenMessageId(latest.getId());
		}
	}

	/**
	 * Updates last seen message id and calls notification listeners if activity status changes. This method
	 * can be used if last seen message is not the latest we have received. Honestly, this probably gets
	 * a bit too over engineered...
	 *
	 * @param messageId
	 */
	public void updateLastSeenMessageId(String messageId) {
		setActivityStatus(ActivityStatus.NONE); // Nah, assume that all messages are seen ..

		if (lastSeenMessageId != null && messageId.compareTo(lastSeenMessageId) <= 0) return; // No op

		lastSeenMessageId = messageId;

		// Send action to server

		UpdateSession a = new UpdateSession();
		a.setMessageId(lastSeenMessageId);
		if (this instanceof Channel) {
			a.setChannelId(getId());
		} else {
			assert this instanceof Dialogue;
			a.setUserId(getId());
		}
		session.getTransport().enqueue(a);

		// Clear activity status

		/*
		SortedSet<Message> tailSet = messages.tailSet(new Message(messageId));
		if (tailSet.size() <= 1) {
			// Latest message have been seen
			setActivityStatus(ActivityStatus.NONE);
		} else {
			// TODO: This doesn't work. It gets complicated if user had new highlights and this update acknowledges them
			// but some unread messages remain. There's no listener method for that event.
			// Anyway, we could now iterate remaining messages in tailset and check if they contain highlightable
			// messages and set activity status accordingly.
		}
		*/
	}

	public String getLastSeenMessageId() {
		return lastSeenMessageId;
	}

	/**
	 * Returns true if there are (new) unseen messages in this conversation
	 *
	 * @return true if unread
	 */
	public boolean isNewMessages() {
		Message latest;
		synchronized (messages) {
			latest = messages.isEmpty() ? null : messages.last();
		}
		if (latest == null) return false;
		if (lastSeenMessageId == null) return true;
		return latest.getId().compareTo(lastSeenMessageId) > 0;
	}

	public ActivityStatus getActivityStatus() {
		return activityStatus;
	}

	/**
	 * <p>Sets activity status and calls activity status listeners if status was changed.</p>
	 * <p><strong>Note:</strong> it is forbidden to go from HIGHLIGHT to UNREAD. It's just ignored.</p>
	 *
	 * @param activityStatus new ActivityStatus
	 */
	void setActivityStatus(ActivityStatus activityStatus) {
		// TODO: Mutex
		if (activityStatus == this.activityStatus) return; // No op

		// No point in going backwards
		if (activityStatus == ActivityStatus.UNREAD && this.activityStatus == ActivityStatus.HIGHLIGHT) return;

		this.activityStatus = activityStatus;

		if (logger.isLoggable(Level.FINE)) logger.fine("Activity status of " + this + " is now " + activityStatus);

		// TODO: This is fking ugly and a probable source of erroneous behavior. There's no guarantee that this method
		// is always called after a new message has been added.
		Message latest = messages.isEmpty() ? null : messages.last();

		for (ActivityStatusListener activityStatusListener : session.getActivityStatusListeners()) {
			switch (activityStatus) {
				case NONE: activityStatusListener.onNone(this); break;
				case UNREAD: activityStatusListener.onUnread(this, latest); break;
				case HIGHLIGHT: activityStatusListener.onHighlight(this, latest); break;
			}
		}
	}

	abstract public String getName();

	public void loadHistory(AckListener ackListener) {
		LoadHistory a = new LoadHistory();

		// Ugly reference to lower level
		if (this instanceof Channel) {
			a.setChannelId(id);

		} else if (this instanceof Dialogue) {
			a.setUserId(id);

		} else {
			throw new UnsupportedOperationException();
		}

		a.setHistoryLength(DEFAULT_HISTORY_SIZE); // TODO: Configurable

		if (!messages.isEmpty()) {
			a.setMessageId(messages.first().getId());
		}

		a.setAckListener(ackListener);

		session.getTransport().enqueue(a);

		historyLoaded = true;
	}

	public String getId() {
		return id;
	}

	public void addConversationListener(ConversationListener conversationListener) {
		conversationListeners.add(conversationListener);
	}

	public void removeConversationListener(ConversationListener conversationListener) {
		conversationListeners.remove(conversationListener);
	}

	void unregisterSessionListener() { }

	public WrappedId getWrappedId() {
		return wrappedId;
	}

	public static abstract class WrappedId implements Serializable {
		protected final String id;

		public static final Pattern pattern = Pattern.compile("^/([pc])/([a-z0-9]+)$");

		protected WrappedId(String id) {
			if (id == null) throw new IllegalArgumentException("Id can not be null!");
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public int hashCode() {
			return id.hashCode();
		}

		public static WrappedId fromString(String s) {
			Matcher m = pattern.matcher(s);

			if (m.matches()) {
				if ("c".equals(m.group(1))) {
					return new Channel.WrappedId(m.group(2));
				} else {
					assert "p".equals(m.group(1));
					return new Dialogue.WrappedId(m.group(2));
				}

			} else {
				throw new IllegalArgumentException("I don't understand: " + s);
			}
		}
	}

	/**
	 * TODO: Exposing payload is probably too low level
	 * TODO: Move to "ConversationUtils" or something
	 *
	 * @param payload
	 */
	public void sendMessage(MessagePayload payload) {
		SendMessage a = new SendMessage();

		// Ugly reference to lower level
		if (this instanceof Channel) {
			a.setChannelId(id);

		} else if (this instanceof Dialogue) {
			a.setUserId(id);

		} else {
			throw new UnsupportedOperationException();
		}

		a.setMessageType(NinchatTextMessage.MESSAGE_TYPE);
		a.setPayloads(new MessagePayload[]{payload});

		session.getTransport().enqueue(a);
	}

	/**
	 * Sends an action to server that removes us from the channel or dialogue.
	 */
	public abstract void leave();

	public boolean isHistoryLoaded() {
		return historyLoaded;
	}

	private boolean containsHighlightTokens(Message message) {
		if (message instanceof PayloadMessage) {
			String text = ((PayloadMessage)message).getText();

			Set<String> highlighTokens = session.getHighlightTokens();

			if (text != null) {
				String[] tokens = text.split(SPLITTER);
				for (String token : tokens) {
					if (token.length() > 0 && highlighTokens.contains(token.toLowerCase())) { // TODO: Locale
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "/" + id + "/" + getName();
	}
}
