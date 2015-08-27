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
import com.ninchat.client.transport.actions.DescribeChannel;
import com.ninchat.client.transport.actions.PartChannel;
import com.ninchat.client.transport.attributes.ChannelAttrs;
import com.ninchat.client.transport.parameters.ChannelMembers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public class Channel extends Conversation {
	private final static Logger logger = Logger.getLogger(Channel.class.getName());

	protected final Map<String, Member> members = new HashMap<String, Member>();

	protected final Collection<ChannelListener> channelListeners = new CopyOnWriteArraySet<ChannelListener>();

	private Realm realm;
	protected String name;
	private String ownerId;
	private boolean accessPrivate; // TODO: Selkeempi nimi
	private boolean accessPublic; // TODO: Selkeempi nimi
	private String topic;
	private boolean suspended;


	/** If channel has imported result of DescribeChannel */
	private boolean described = false;

	private final SessionListener sessionListener = new SessionListener() {
		@Override
		public void onUserUpdated(Session session, User user) {
			Member member = members.get(user.getUserId());

			if (member != null) {
				for (ChannelListener channelListener : channelListeners) {
					channelListener.onMemberUpdated(Channel.this, member);
				}
			}
		}
	};

	public Channel(Session session, String id, Realm realm) {
		super(session, id, new WrappedId(id));
		this.realm = realm;

		if (session != null) {
			session.addSessionListener(sessionListener); // TODO: removal when channel is removed
		}
	}

	void unregisterSessionListener() {
		session.removeSessionListener(sessionListener);
	}

	public void addChannelListener(ChannelListener channelAdapter) {
		channelListeners.add(channelAdapter);
	}

	public void removeChannelListener(ChannelListener channelListener) {
		channelListeners.remove(channelListener);
	}

	public Collection<ChannelListener> getChannelListeners() {
		return channelListeners;
	}

	public Map<String, Member> getMembers() {
		return members;
	}

	boolean updateMember(Member member) {
		members.put(member.getUser().getUserId(), member); // TODO: Identity check

		for (ChannelListener channelListener : channelListeners) {
			channelListener.onMemberUpdated(this, member);
		}

		return true;
	}

	boolean removeMember(String userId) {
		Member member = members.remove(userId);

		if (member != null) {
			for (ChannelListener channelListener : channelListeners) {
				channelListener.onMemberParted(this, member);
			}
			return true;
		}

		return false;
	}

	boolean addMember(Member member) {
		synchronized (members) {
			members.put(member.getUser().getUserId(), member);
		}

		for (ChannelListener channelListener : channelListeners) {
			channelListener.onMemberJoined(this, member);
		}

		return true;
	}

	void importChannelAttrs(ChannelAttrs attrs) {
		name = attrs.getName();
		topic = attrs.getTopic();
		ownerId = attrs.getOwnerId();
		accessPrivate = attrs.getPrivate();
		accessPublic = attrs.getPublic();
		suspended = attrs.getSuspended();
	}

	public boolean isDescribed() {
		return described;
	}

	void importChannelMembers(ChannelMembers channelMembers) {
		described = true; // TODO: Not sure if this is right place

		synchronized (members) {
			members.clear();

			for (Map.Entry<String, ChannelMembers.Parameters> entry : channelMembers.entrySet()) {
				String userId = entry.getKey();
				ChannelMembers.Parameters parameters = entry.getValue();

				User user = session.getOrCreateUser(userId);
				user.importUserAttrs(parameters.getUserAttrs());

				Member membership = new Member(user);
				if (parameters.getMemberAttrs() != null) {
					membership.importMemberAttrs(parameters.getMemberAttrs());
				}

				members.put(userId, membership);
			}
		}
	}

	public void describeChannel(AckListener ackListener) {

		DescribeChannel a = new DescribeChannel();
		a.setChannelId(id);
		a.setAckListener(ackListener);
		session.getTransport().enqueue(a);
	}

	public Realm getRealm() {
		return realm;
	}

	public void setRealm(Realm realm) {
		this.realm = realm;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public boolean isAccessPrivate() {
		return accessPrivate;
	}

	public void setAccessPrivate(boolean accessPrivate) {
		this.accessPrivate = accessPrivate;
	}

	public boolean isAccessPublic() {
		return accessPublic;
	}

	public void setAccessPublic(boolean accessPublic) {
		this.accessPublic = accessPublic;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public void leave() {
		PartChannel a = new PartChannel();
		a.setChannelId(id);
		session.getTransport().enqueue(a);
	}

	public static class WrappedId extends Conversation.WrappedId {
		public WrappedId(String id) {
			super(id);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof WrappedId && ((WrappedId)obj).id.equals(id));
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public String toString() {
			return "/c/" + id;
		}
	}
}
