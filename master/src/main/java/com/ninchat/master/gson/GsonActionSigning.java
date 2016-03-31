/*
 * Copyright (c) 2016, Somia Reality Oy
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
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.ninchat.master.gson;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import com.ninchat.master.SigningKey;

/**
 * Utilities for creating action signatures.  The methods return values for the
 * master_sign parameters found in Ninchat API.
 *
 * The signatures may be used once before the expiration time.  Expiration time
 * is specified in Unix time (seconds since 1970-01-01 UTC), and may not be
 * more than one week in the future.
 *
 * Example (Java 8):
 *
 * <pre>
 * {@code
 *     import java.util.Base64;
 *     import com.ninchat.master.SigningKey;
 *     import com.ninchat.master.gson.GsonActionSigning;
 *
 *     // do once:
 *
 *     SigningKey key = new SigningKey(
 *             masterKeyId,
 *             Base64.getDecoder().decode(masterKeySecret),
 *             (byte[] x) -> Base64.getEncoder().encodeToString(x));
 *
 *     // do repeatedly:
 *
 *     long expire = System.currentTimeMillis() / 1000 + sessionExpireTimeout;
 *
 *     String masterSign = GsonActionSigning.signJoinChannel(key, expire, theChannelId);
 * }
 * </pre>
 *
 * @see org.apache.commons.codec.binary.Base64
 */
public class GsonActionSigning
{
	/**
	 * Generate a signature for use with a create_session API call, when
	 * creating a new user.  The created user will become a puppet of the
	 * master.
	 */
	public static String signCreateSession(SigningKey key, long expire) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return doSignCreateSession(key, expire, null);
	}

	/**
	 * Generate a signature for use with a create_session API call, when
	 * authenticating an existing user.  The user must be a puppet of the
	 * master.
	 *
	 * @param userId must be repeated in the API call.
	 */
	public static String signCreateSessionForUser(SigningKey key, long expire, String userId) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return doSignCreateSession(key, expire, userId);
	}

	private static final String doSignCreateSession(SigningKey key, long expire, String userId) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		String nonce = key.makeNonce();

		JsonArray msg = new JsonArray();
		addEntry(msg, "action", new JsonPrimitive("create_session"));
		addEntry(msg, "expire", new JsonPrimitive(expire));
		addEntry(msg, "nonce", new JsonPrimitive(nonce));

		if (userId != null)
			addEntry(msg, "user_id", new JsonPrimitive(userId));

		return key.sign(expire, nonce, msg.toString().getBytes("UTF-8"));
	}

	/**
	 * Generate a signature for use with a join_channel API call.  The
	 * master must own the channel.
	 *
	 * @param channelId must be repeated in the API call.
	 */
	public static String signJoinChannel(SigningKey key, long expire, String channelId) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return signJoinChannel(key, expire, channelId, null);
	}

	/**
	 * Generate a signature for use with a join_channel API call.  The
	 * master must own the channel.
	 *
	 * @param channelId must be repeated in the API call.
	 * @param memberAttrs is an array of key-value pairs (arrays with two
	 *                    elements).  It must already have been sorted by the
	 *                    key (the first element of the pair).  It must be
	 *                    repeated in the API call (as an object instead of an
	 *                    array).
	 */
	public static String signJoinChannel(SigningKey key, long expire, String channelId, JsonElement memberAttrs) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return doSignJoinChannel(key, expire, channelId, null, memberAttrs);
	}

	/**
	 * Generate a signature for use with a join_channel API call, by the
	 * specified user only.  The master must own the channel.  (Note that the
	 * userId is not repeated in the API call.)
	 *
	 * @param channelId must be repeated in the API call.
	 * @param userId is NOT repeated in the API call (it is deduced from the
	 *               session or authentication credentials).
	 */
	public static String signJoinChannelForUser(SigningKey key, long expire, String channelId, String userId) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return signJoinChannelForUser(key, expire, channelId, userId, null);
	}

	/**
	 * Generate a signature for use with a join_channel API call, by the
	 * specified user only.  The master must own the channel.
	 *
	 * @param channelId must be repeated in the API call.
	 * @param userId is NOT repeated in the API call (it is deduced from the
	 *               session or authentication credentials).
	 * @param memberAttrs is an array of key-value pairs (arrays with two
	 *                    elements).  It must already have been sorted by the
	 *                    key (the first element of the pair).  It must be
	 *                    repeated in the API call (as an object instead of an
	 *                    array).
	 */
	public static String signJoinChannelForUser(SigningKey key, long expire, String channelId, String userId, JsonElement memberAttrs) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return doSignJoinChannel(key, expire, channelId, userId, memberAttrs) + "-1";
	}

	private static final String doSignJoinChannel(SigningKey key, long expire, String channelId, String userId, JsonElement memberAttrs) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		String nonce = key.makeNonce();

		JsonArray msg = new JsonArray();
		addEntry(msg, "action", new JsonPrimitive("join_channel"));
		addEntry(msg, "channel_id", new JsonPrimitive(channelId));
		addEntry(msg, "expire", new JsonPrimitive(expire));

		if (memberAttrs != null)
			addEntry(msg, "member_attrs", memberAttrs);

		addEntry(msg, "nonce", new JsonPrimitive(nonce));

		if (userId != null)
			addEntry(msg, "user_id", new JsonPrimitive(userId));

		return key.sign(expire, nonce, msg.toString().getBytes("UTF-8"));
	}

	private static final void addEntry(JsonArray msg, String key, JsonElement value)
	{
		JsonArray pair = new JsonArray();
		pair.add(new JsonPrimitive(key));
		pair.add(value);
		msg.add(pair);
	}

	private GsonActionSigning() {}
}
