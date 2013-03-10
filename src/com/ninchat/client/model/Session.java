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

import com.ninchat.client.transport.*;
import com.ninchat.client.transport.actions.*;
import com.ninchat.client.transport.attributes.RealmAttrs;
import com.ninchat.client.transport.events.*;
import com.ninchat.client.transport.events.Error;
import com.ninchat.client.transport.parameters.UserChannels;
import com.ninchat.client.transport.payloads.MessagePayload;
import com.ninchat.client.transport.payloads.NinchatTextMessage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public class Session {
	private final static Logger logger = Logger.getLogger(Session.class.getName());

	public enum Status {
		VIRGIN,
		ESTABLISHING,
		ESTABLISHED,
	}

	private Status status = Status.VIRGIN;

	private final AbstractTransport transport;

	private String sessionId;
	private String userId;
	private String userAuth;

	private User sessionUser;

	private SessionCreationMethod sessionCreationMethod;

	private boolean idle;

	private final Map<String, Channel> channels = new HashMap<String, Channel>();
	private final Map<String, Dialogue> dialogues = new HashMap<String, Dialogue>();

	private final Map<String, Realm> realms = new HashMap<String, Realm>();
	private final Set<Realm> userRealms = new HashSet<Realm>();

	private final Map<String, User> users = new HashMap<String, User>();

	private final Set<SessionListener> sessionListeners = new CopyOnWriteArraySet<SessionListener>(); // Synchronization not required
	private final Set<ActivityStatusListener> activityStatusListeners = new CopyOnWriteArraySet<ActivityStatusListener>();

	private final Map<Conversation, List<Message>> openMessageBundles = new HashMap<Conversation, List<Message>>();

	private final Set<String> highlightTokens = new CopyOnWriteArraySet<String>();

	private boolean autoEstablish = true;

	public Session(AbstractTransport transport) {
		this.transport = transport;

		transport.addEventListener(SessionCreated.class, new SessionCreatedListener());
		transport.addEventListener(HistoryResults.class, new HistoryResultsListener());
		transport.addEventListener(MessageReceived.class, new MessageReceivedListener());
		transport.addEventListener(ChannelFound.class, new ChannelFoundListener());
		transport.addEventListener(RealmFound.class, new RealmFoundListener());
		transport.addEventListener(UserFound.class, new UserFoundListener());
		transport.addEventListener(UserUpdated.class, new UserUpdatedListener());
		transport.addEventListener(ChannelMemberUpdated.class, new ChannelMemberUpdatedListener());
		transport.addEventListener(ChannelMemberJoined.class, new ChannelMemberJoinedListener());
		transport.addEventListener(ChannelMemberParted.class, new ChannelMemberPartedListener());
		transport.addEventListener(Error.class, new ErrorListener());

		transport.addTransportStatusListener(new SessionTransportStatusListener());
	}

	public SessionCreationMethod getSessionCreationMethod() {
		return sessionCreationMethod;
	}

	public void setSessionCreationMethod(SessionCreationMethod sessionCreationMethod) {
		this.sessionCreationMethod = sessionCreationMethod;
	}

	public AbstractTransport getTransport() {
		return transport;
	}

	public Status getStatus() {
		return status;
	}

	public boolean isEstablished() {
		return status == Status.ESTABLISHED;
	}

	@Deprecated
	public void init() {

	}

	public Map<String, User> getUsers() {
		return users;
	}

	public User getUser(String userId) {
		synchronized (users) {
			return users.get(userId);
		}
	}

	public User getOrCreateUser(String userId) {
		synchronized (users) {
			User user = users.get(userId);
			if (user == null) {
				user = new User(userId);
				users.put(userId, user);

				if (logger.isLoggable(Level.FINE)) logger.fine("Created new User object: " + userId);
			}

			return user;
		}
	}

	public Dialogue getOrCreateDialogue(String userId) {
		synchronized (dialogues) {
			Dialogue dialogue = dialogues.get(userId);
			if (dialogue == null) {
				dialogue = new Dialogue(this, userId);
				dialogues.put(userId, dialogue);

				if (logger.isLoggable(Level.FINE)) logger.fine("Created new Dialogue: " + userId);
			}

			return dialogue;
		}
	}

	public Realm getOrCreateRealm(String realmId) {
			synchronized (realms) {
			Realm realm = realms.get(realmId);
			if (realm == null) {
				realm = new Realm(realmId);
				realms.put(realmId, realm);

				if (logger.isLoggable(Level.FINE)) logger.fine("Created new Realm: " + realmId);
			}

			return realm;
		}
	}

	public void addSessionListener(SessionListener sessionListener) {
		sessionListeners.add(sessionListener);
	}

	public void removeSessionListener(SessionListener sessionListener) {
		sessionListeners.remove(sessionListener);
	}

	Set<SessionListener> getSessionListeners() {
		return sessionListeners;
	}


	public void addActivityStatusListener(ActivityStatusListener activityStatusListener) {
		activityStatusListeners.add(activityStatusListener);
	}

	public void removeActivityStatusListener(ActivityStatusListener activityStatusListener) {
		activityStatusListeners.remove(activityStatusListener);
	}

	Set<ActivityStatusListener> getActivityStatusListeners() {
		return activityStatusListeners;
	}

	/**
	 * Returns most important status for all conversations. Note: UNREAD on Dialogue is counted as HIGHLIGHT
	 *
	 * @return
	 */
	public Channel.ActivityStatus getActivityStatus() {
		Conversation.ActivityStatus status = Conversation.ActivityStatus.NONE;

		for (Conversation c : getConversations()) {
			Conversation.ActivityStatus a = c.getActivityStatus();
			if (a == Conversation.ActivityStatus.UNREAD && c instanceof Dialogue) a = Conversation.ActivityStatus.HIGHLIGHT;
			if (a.getPriority() > status.getPriority()) {
				status = a;
			}
		}

		return status;
	}

	public int getConversationsWithActivitiesCount() {
		int count = 0;
		for (Conversation c : getConversations()) {
			if (c.getActivityStatus() != Conversation.ActivityStatus.NONE) {
				count++;
			}
		}

		return count;
	}

	public void describeRealm(String realmId, AckListener ackListener) {
		DescribeRealm a = new DescribeRealm();
		a.setRealmId(realmId);
		a.setAckListener(ackListener);
		transport.enqueue(a);
	}

	public void describeUser(String userId, AckListener ackListener) {
		DescribeUser a = new DescribeUser();
		a.setUserId(userId);
		a.setAckListener(ackListener);
		transport.enqueue(a);
	}

	public boolean sendTextMessage(String channelId, String text) {

		NinchatTextMessage m = new NinchatTextMessage();
		m.setText(text);

		SendMessage a = new SendMessage();
		a.setChannelId(channelId);
		a.setMessageType(NinchatTextMessage.MESSAGE_TYPE);
		a.setPayloads(new Payload[]{m});

		transport.enqueue(a);

		return true; // TODO: Really?
	}

	public void startSession() {
		if (status != Status.VIRGIN) {
			throw new IllegalStateException("Only virgin session can be started");
		}

		if (sessionCreationMethod == null) {
			throw new IllegalStateException("Starting session but no sessionCreationMethod defined!");
		}

		CreateSession a = sessionCreationMethod.getAction();
		a.setMessageTypes(new String [] { "*" });
		transport.enqueue(a);

		status = Status.ESTABLISHING;
	}

	/**
	 * Initiates graceful session shutdown.
	 */
	public void endSession() {
		if (status == Status.ESTABLISHED) {
			if (transport.isConnected()) {
				logger.fine("Sending close_session");

				transport.enqueue(new CloseSession());
			}

			try {
				// Take a nap before termination. Give transport some time for transmitting the action
				Thread.sleep(200);
			} catch (InterruptedException e) {  }

			// .. And ensure that it really ends
			terminate();

		} else {
			logger.warning("Session is not established. Can not end session!");
		}
	}

	/**
	 * Terminates (invalidates) this session and returns it to VIRGIN state.
	 */
	public void terminate() {
		if (status == Status.VIRGIN) {
			logger.info("terminateSession(): Session is still virgin. No point in termination");
			return;

		} else if (status == Status.ESTABLISHING) {
			throw new IllegalStateException("Oh no! What to do! Trying to terminate an ESTABLISHING session. Fixing this would be a good idea.");
		}

		realms.clear();
		users.clear();

		for (Channel channel : channels.values()) {
			channel.unregisterSessionListener();
		}
		channels.clear();

		for (Dialogue dialogue : dialogues.values()) {
			dialogue.unregisterSessionListener();
		}
		channels.clear();

		highlightTokens.clear();

		status = Status.VIRGIN;

		transport.setSessionId(null);
		transport.terminate();

		for (SessionListener sessionListener : sessionListeners) {
			sessionListener.onSessionEnded(Session.this);
		}
	}

	private class HistoryResultsListener implements TransportEventListener<HistoryResults> {
		@Override
		public void onEvent(HistoryResults event) {
			String channelId = event.getChannelId();
			String userId = event.getUserId();

			Conversation target;

			if (channelId != null) {
				Channel channel = channels.get(channelId);
				if (channel == null) {
					logger.warning("History for unknown chat. Ignoring.");
					return;
				}

				target = channel;

			} else if (userId != null) {
				Dialogue dialogue = getOrCreateDialogue(userId);
				target = dialogue;

			} else {
				logger.warning("Message has no channelId or userId. Can do nothing with it!");
				return;
			}

			openMessageBundles.put(target, new ArrayList<Message>());
		}
	}

	private class MessageReceivedListener implements TransportEventListener<MessageReceived> {
		@Override
		public void onEvent(MessageReceived event) {

			Message message = null;

			MessagePayload payload = (MessagePayload)event.getPayloads()[0]; // Assume single payload for now...

			// TODO: More dynamic solution
			if (payload instanceof NinchatTextMessage) {
				message = new TextMessage(event);

			} else {
				logger.fine("Unsupported message type: " + payload.getClass().getName());
			}

			String channelId = event.getChannelId();
			String userId = event.getUserId();
			Conversation target;

			if (channelId != null) {
				Channel channel = channels.get(channelId);
				if (channel == null) {
					logger.warning("Message for unknown channel. Ignoring.");
					return;
				}

				target = channel;

			} else if (userId != null) {
				Dialogue dialogue = getOrCreateDialogue(userId);
				target = dialogue;

				if (!dialogue.getPeer().isLoaded()) {
					describeUser(dialogue.getPeer().getUserId(), null); // TODO: Refresh gui when ready
				}

			} else {
				logger.warning("Message has no channelId or userId. Can do nothing with it!");
				return;
			}


			if (event.getHistoryLength() == null) {
				if (message != null) {
					target.addMessage(message);
				}

			} else {
				List<Message> currentHistoryBundle = openMessageBundles.get(target);

				if (currentHistoryBundle == null) {
					logger.warning("Receiving history message, but no bundle is allocated for it! Ignoring. Event " + event);
					return;
				}

				int remaining = (int)(long)event.getHistoryLength();
				if (message != null) {
					currentHistoryBundle.add(message);
				}

				if (remaining <= 0) {
					if (!currentHistoryBundle.isEmpty()) {
						Collections.sort(currentHistoryBundle);
						target.addMessages(currentHistoryBundle);
					}

					openMessageBundles.remove(target);
				}
			}
		}
	}

	private class SessionCreatedListener implements TransportEventListener<SessionCreated> {
		@Override
		public void onEvent(SessionCreated event) {

			transport.setHost(event.getSessionHost());

			sessionId = event.getSessionId();
			userId = event.getUserId();
			userAuth = event.getUserAuth();

			sessionUser = getOrCreateUser(event.getUserId());
			sessionUser.importUserAttrs(event.getUserAttrs());

			highlightTokens.add(sessionUser.getName().toLowerCase()); // TODO: Others?, Locale

			if (event.getUserRealms() != null) {
				for (Map.Entry<String, RealmAttrs> entry : event.getUserRealms().entrySet()) {
					Realm realm = getOrCreateRealm(entry.getKey());
					realm.importRealmAttrs(entry.getValue());
					userRealms.add(realm);
				}
			}

			if (event.getUserChannels() != null) {
				for (Map.Entry<String, UserChannels.Parameters> entry : event.getUserChannels().entrySet()) {
					String channelId = entry.getKey();
					UserChannels.Parameters parameters = entry.getValue();

					Realm realm = null;
					String realmId = parameters.getRealmId();
					if (realmId != null) {
						realm = getOrCreateRealm(realmId);
					}
					Channel channel = new Channel(Session.this, channelId, realm);

					if (parameters.getChannelAttrs() != null) {
						channel.importChannelAttrs(parameters.getChannelAttrs());
					}

					// TODO: channel_status

					channels.put(channelId, channel);

					logger.info("Created channel: " + channel.getId() + " / " + channel.getName()); // ??
				}
			}

			if (event.getUserDialogues() != null) {
				for (Map.Entry<String, Object> entry : event.getUserDialogues().entrySet()) {
					String peerId = entry.getKey();
//					String attribute = entry.getValue(); // TODO: Highlights

					Dialogue dialogue = getOrCreateDialogue(peerId);
					logger.info("Created dialogue: " + dialogue.getId() + " / " + dialogue.getName()); // ??
				}
			}

			// TODO: user_identities
			// TODO: user_account
			// TODO: user_settings

			status = Status.ESTABLISHED;

			transport.setSessionId(sessionId); // For resume_session

			for (SessionListener sessionListener : sessionListeners) {
				sessionListener.onSessionEstablished(Session.this);
			}

			loadAttributes();

			autoEstablish = true;
		}
	}

	private class ChannelFoundListener implements TransportEventListener<ChannelFound> {
		@Override
		public void onEvent(ChannelFound event) {
			String channelId = event.getChannelId();

			Channel channel = channels.get(channelId);
			if (channel == null) {
				logger.fine("channel_found for unknown channel: " + channelId);
				return;
			}

			// TODO: channel_attrs
			// TODO: channel_status
			// TODO: realm_id

			if (event.getChannelMembers() != null) {
				channel.importChannelMembers(event.getChannelMembers());
				for (ChannelListener channelListener : channel.getChannelListeners()) {
					channelListener.onMembersUpdated(channel);
				}
			}
		}
	}

	private class UserFoundListener implements TransportEventListener<UserFound> {
		@Override
		public void onEvent(UserFound event) {
			User user = getOrCreateUser(event.getUserId());
			user.importUserAttrs(event.getUserAttrs());

			for (SessionListener sessionListener : sessionListeners) {
				sessionListener.onUserUpdated(Session.this, user); // TODO: Catch
			}

		}
	}

	private class RealmFoundListener implements TransportEventListener<RealmFound> {
		@Override
		public void onEvent(RealmFound event) {
			Realm realm = getOrCreateRealm(event.getRealmId());
			realm.importRealmAttrs(event.getRealmAttrs());
		}
	}

	// TODO: Tää on vähä copypastee edellisen kanssa
	private class UserUpdatedListener implements TransportEventListener<UserUpdated> {
		@Override
		public void onEvent(UserUpdated event) {

			User user = getOrCreateUser(event.getUserId());
			user.importUserAttrs(event.getUserAttrs());

			for (SessionListener sessionListener : sessionListeners) {
				sessionListener.onUserUpdated(Session.this, user); // TODO: Catch
			}

			// TODO: user_settings
			// TODO: user_account
		}
	}

	private class ChannelMemberUpdatedListener implements TransportEventListener<ChannelMemberUpdated> {
		@Override
		public void onEvent(ChannelMemberUpdated event) {

			Channel channel = channels.get(event.getChannelId());
			if (channel != null) {
				Member member = channel.getMembers().get(event.getUserId());
				if (member != null) {
					member.importMemberAttrs(event.getMemberAttrs());
					channel.updateMember(member);
				}
			}
		}
	}

	private class ChannelMemberJoinedListener implements TransportEventListener<ChannelMemberJoined> {
		@Override
		public void onEvent(ChannelMemberJoined event) {

			User user = getOrCreateUser(event.getUserId());
			user.importUserAttrs(event.getUserAttrs());

			Channel channel = channels.get(event.getChannelId());
			if (channel != null) {
				Member member = channel.getMembers().get(user.getUserId());
				if (member == null) {
					member = new Member(user);
					member.importMemberAttrs(event.getMemberAttrs());
					channel.addMember(member);
				}
			}
		}
	}

	private class ChannelMemberPartedListener implements TransportEventListener<ChannelMemberParted> {
		@Override
		public void onEvent(ChannelMemberParted event) {
			User user = users.get(event.getUserId());
			if (user != null) {
				Channel channel = channels.get(event.getChannelId());
				if (channel != null) {
					channel.removeMember(event.getUserId());
				}
			}
		}
	}

	private class ErrorListener implements TransportEventListener<Error> {
		@Override
		public void onEvent(Error event) {
			/*
			String errorType = event.getErrorType();
			if ("session_not_found".equals(errorType)) {
				terminate(); // TODO: Establish again using userAuth
				return;
			}
			*/

			// TODO: Have to figure out which error codes mean permanent condition (no point in reconnecting)

			if (!isEstablished()) {
				// Encountered an error while establishing the session. It's probably permanent. TODO: Or is it?
				autoEstablish = false;
			}

			for (SessionListener sessionListener : sessionListeners) {
				sessionListener.onError(Session.this, event);
			}
		}
	}

	private class SessionTransportStatusListener implements TransportStatusListener {
		@Override
		public void onOpen(AbstractTransport transport) { }

		@Override
		public void onOpening(AbstractTransport transport) { }

		@Override
		public void onInvalidSession(AbstractTransport transport) {
			terminate();

			autoEstablish = false;
			// If start session is successful, autoEstablish will be set to true
			// It will remain false if an error occures
			if (sessionCreationMethod instanceof UserIdSessionCreationMethod) {
				startSession();
			}
		}

		@Override
		public void onClose(AbstractTransport transport) {
			if (status == Status.ESTABLISHING) {
				// Server closed connection during log in process. I'm still virgin!
				status = Status.VIRGIN;
			}
		}
	}

	public boolean isIdle() {
		return idle;
	}

	public void setIdle(boolean idle) {
		if (this.idle == idle) return; // No op

		this.idle = idle;

		UpdateSession a = new UpdateSession();
		a.setSessionIdle(idle);
		transport.enqueue(a);
	}

	public String getSessionId() {
		return sessionId;
	}

	@Deprecated
	public String getSessionHost() {
		return transport.getHost();
	}

	public String getUserId() {
		return userId;
	}

	public String getUserAuth() {
		return userAuth;
	}

	public Map<String, Channel> getChannels() {
		return channels;
	}

	public Map<String, Dialogue> getDialogues() {
		return dialogues;
	}

	public Set<String> getHighlightTokens() {
		return highlightTokens;
	}

	/**
	 * Returns an immutable list that contains channels and dialogues in sorted order.
	 * <p>
	 * <strong>Note: </strong> Currently this method creates a new list on every method call and it is highly
	 * discouraged to call this multiple times inside a loop.
	 *
	 * @return Sorted conversations
	 */
	public List<Conversation> getConversations() {
		ArrayList<Conversation> conversations = new ArrayList<Conversation>();
		conversations.addAll(channels.values());
		conversations.addAll(dialogues.values());
		Collections.sort(conversations, new ChatComparator());
		return Collections.unmodifiableList(conversations);
	}

	public Map<String, Realm> getRealms() {
		return realms;
	}

	public User getSessionUser() {
		return sessionUser;
	}


	private class AttributesLoadedAckListener extends SimpleAckListener {
		volatile int pending = 0;

		@Override
		public void onReady(boolean success) {
			pending--;
			if (pending == 0) {
				for (SessionListener sessionListener : sessionListeners) {
					sessionListener.onAttributesLoaded(Session.this);
				}
			}
		}

		public void addPending() {
			pending++;
		}
	}

	/**
	 * Load attributes and call onAttributesLoaded when all attributes have been received
	 */
	private void loadAttributes() {
		AttributesLoadedAckListener ack = new AttributesLoadedAckListener();

		boolean somethingToLoad = false;

		for (Realm realm : realms.values()) {
			if (!realm.isDescribed()) {
				somethingToLoad = true;
				ack.addPending();
				describeRealm(realm.getId(), ack);
			}
		}

		for (Dialogue dialogue : dialogues.values()) {
			if (!dialogue.getPeer().isLoaded()) {
				somethingToLoad = true;
				ack.addPending();
				describeUser(dialogue.getPeer().getUserId(), ack);
			}
		}

		if (!somethingToLoad) {
			for (SessionListener sessionListener : sessionListeners) {
				sessionListener.onAttributesLoaded(Session.this);
			}
		}
	}

}
