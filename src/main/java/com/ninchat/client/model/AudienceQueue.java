package com.ninchat.client.model;

import com.ninchat.client.transport.parameters.QueueAttrs;

/**
 * @author Kari
 */
public class AudienceQueue {
	private final String id;
	private String name;
	private Realm realm;
	private int length;
	private boolean closed;

	public AudienceQueue(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	public Realm getRealm() {
		return realm;
	}

	void setRealm(Realm realm) {
		this.realm = realm;
	}

	public int getLength() {
		return length;
	}

	void setLength(int length) {
		this.length = length;
	}

	public boolean isClosed() {
		return closed;
	}

	void setClosed(boolean closed) {
		this.closed = closed;
	}

	public void importAttrs(QueueAttrs queueAttrs) {
		this.name = "" + queueAttrs.getName();
		this.length = queueAttrs.getLength();
		this.closed = queueAttrs.isClosed();
	}

	@Override
	public String toString() {
		return "AudienceQueue{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", realm=" + realm +
				", length=" + length +
				", closed=" + closed +
				'}';
	}
}
