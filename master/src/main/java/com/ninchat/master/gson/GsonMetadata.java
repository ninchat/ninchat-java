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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.ninchat.master.EncryptionKey;

/**
 * Utilities for creating secure metadata.  The methods return values for the
 * "secure" property of the audience_metadata parameter found in Ninchat API.
 *
 * The secured metadata may be used once before the expiration time.
 * Expiration time is specified in Unix time (seconds since 1970-01-01 UTC),
 * and may not be more than one week in the future.
 *
 * Example (Java 8):
 *
 * <pre>
 * {@code
 *     import java.util.Base64;
 *     import com.google.gson.JsonObject;
 *     import com.ninchat.master.EncryptionKey;
 *     import com.ninchat.master.gson.GsonMetadata;
 *
 *     // do once:
 *
 *     EncryptionKey key = new EncryptionKey(
 *             masterKeyId,
 *             Base64.getDecoder().decode(masterKeySecret),
 *             (byte[] x) -> Base64.getEncoder().encodeToString(x));
 *
 *     // do repeatedly:
 *
 *     JsonObject metadata = new JsonObject();
 *     metadata.addProperty("Customer name", customerName);
 *     metadata.addProperty("Shoe size", shoeSize);
 *
 *     long expire = System.currentTimeMillis() / 1000 + sessionExpireTimeout;
 *
 *     String secureMetadata = GsonMetadata.secure(key, expire, metadata);
 * }
 * </pre>
 *
 * @see org.apache.commons.codec.binary.Base64
 */
public class GsonMetadata
{
	/**
	 * Encrypt metadata for use with a request_audience API call.
	 */
	public static String secure(EncryptionKey key, Number expire, JsonElement metadata) throws BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException
	{
		JsonObject msg = new JsonObject();
		msg.addProperty("expire", expire);
		msg.add("metadata", metadata);

		return key.secureMetadata(msg.toString().getBytes("UTF-8"));
	}

	/**
	 * Encrypt metadata for use with a request_audience API call, by the
	 * specified user only.
	 */
	public static String secureForUser(EncryptionKey key, Number expire, JsonElement metadata, String userId) throws BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException
	{
		JsonObject msg = new JsonObject();
		msg.addProperty("expire", expire);
		msg.add("metadata", metadata);
		msg.addProperty("user_id", userId);

		return key.secureMetadata(msg.toString().getBytes("UTF-8"));
	}

	private GsonMetadata() {}
}
