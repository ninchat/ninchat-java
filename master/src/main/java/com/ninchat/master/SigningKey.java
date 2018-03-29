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

package com.ninchat.master;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Holds a Ninchat master key, and knows how to create HMAC signatures for use
 * with Ninchat API actions.
 *
 * The master key id and secret may be obtained with the create_master_key
 * Ninchat API action.
 *
 * The masterKeySecretData passed to the constructor is binary data; it must
 * have been base64-decoded beforehand.
 *
 * @see com.ninchat.master.gson.GsonActionSigning
 */
public class SigningKey
{
	private static final String ALGORITHM = "HmacSHA512";
	private static final int NONCE_SIZE = 6; // Doesn't cause ='s at the end

	private final String prefix;
	private final SecretKeySpec keySpec;
	private final Base64Encoder encoder;
	private final SecureRandom random;

	public SigningKey(String masterKeyId, byte[] masterKeySecretData, Base64Encoder encoder)
	{
		this(masterKeyId, masterKeySecretData, encoder, new SecureRandom());
	}

	public SigningKey(String masterKeyId, byte[] masterKeySecretData, Base64Encoder encoder, SecureRandom random)
	{
		prefix = masterKeyId + ".";
		keySpec = new SecretKeySpec(masterKeySecretData, ALGORITHM);
		this.encoder = encoder;
		this.random = random;
	}

	/**
	 * This is a low-level utility.  Use only when implementing an alternative
	 * for GsonActionSigning.
	 *
	 * @return a random token to be used as part of the signed JSON message.
	 */
	public String makeNonce()
	{
		byte[] data = new byte[NONCE_SIZE];
		random.nextBytes(data);

		return encoder.encode(data).replace('+', '-').replace('/', '_');
	}

	/**
	 * This is a low-level interface.  Use only when implementing an
	 * alternative for GsonActionSigning.
	 *
	 * @param expire is seconds since 1970-01-01 UTC.
	 * @param nonce was created with makeNonce().
	 * @param msg is JSON-encoded text.
	 * @param flags is "" or "1".
	 *
	 * @return a value suitable to be passed to the Ninchat API as the
	 *         master_sign parameter.
	 */
	public String sign(long expire, String nonce, byte[] msg, String flags) throws InvalidKeyException, NoSuchAlgorithmException
	{
		Mac hmac = Mac.getInstance(ALGORITHM);
		hmac.init(keySpec);
		byte[] digest = hmac.doFinal(msg);
		String digestBase64 = encoder.encode(digest);

		return prefix + Long.toString(expire) + "." + nonce + "." + digestBase64 + "." + flags;
	}
}
