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

import com.ninchat.client.transport.attributes.UserAttrs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kari Lavikka
 */
public class User {
	private final static Logger logger = Logger.getLogger(User.class.getName());

	protected final String userId;
	protected Set identities; // ???

	private boolean admin;
	private boolean connected;
	private URL iconURL;
	private long lastAction; // Tästä voidaan laskea tän hetkinen idle
	//private void info;
	private String name;
	private String realName;
	private boolean guest;
	private Long idle;

	/**
	 * Pseudo attribute for a deleted user that does not exist on the server any more
	 */
	private boolean deleted;

	private boolean imported = false;

	public enum Presence {
		OFFLINE,
		IDLE,
		ONLINE
	}

	User(String userId) {
		this.userId = userId;
		this.name = userId;
	}

	void importUserAttrs(UserAttrs attrs) {
		assert attrs != null;

		imported = true;

		admin = attrs.getAdmin();
		connected = attrs.getConnected();
		guest = attrs.getGuest();

		idle = attrs.getIdle();
		if (idle != null) idle *= 1000;

		// TODO: info

		if (attrs.getName() != null) {
			name = attrs.getName();
		}
		realName = attrs.getRealname();

		if (attrs.getIconurl() != null) {
			try {
				URL url = new URL(attrs.getIconurl());
				iconURL = url;
			} catch (MalformedURLException e) {
				logger.log(Level.FINER, "Malformed URL for user " + userId + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Returns true if attributes have been loaded to this User
	 *
	 * @return true if loaded
	 */
	public boolean isLoaded() {
		return imported;
	}

	public String getUserId() {
		return userId;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public URL getIconURL() {
		return iconURL;
	}

	public void setIconURL(URL iconURL) {
		this.iconURL = iconURL;
	}

	public long getLastAction() {
		return lastAction;
	}

	public void setLastAction(long lastAction) {
		this.lastAction = lastAction;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public boolean isGuest() {
		return guest;
	}

	public void setGuest(boolean guest) {
		this.guest = guest;
	}

	public Long getIdle() {
		return idle;
	}

	public void setIdle(Long idle) {
		this.idle = idle;
	}

	public boolean isDeleted() {
		return deleted;
	}

	void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Presence getPresence() {
		if (!connected) {
			return Presence.OFFLINE;
		} else if (idle == null) {
			return Presence.ONLINE;
		} else {
			return Presence.IDLE;
		}
	}

	@Override
	public int hashCode() {
		return userId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof User && userId.equals(((User)obj).userId);
	}

	@Override
	public String toString() {
		return "User{" +
				"userId='" + userId + '\'' +
				", name='" + name + '\'' +
				", realName='" + realName + '\'' +
				'}';
	}
}
