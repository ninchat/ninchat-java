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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Holds a Ninchat master key, and knows how to convert JSON data into a string
 * suitable for the "secure" metadata property.
 *
 * The master key id and secret may be obtained with the create_master_key
 * Ninchat API action.
 *
 * The masterKeySecretData passed to the constructor is binary data; it must
 * have been base64-decoded beforehand.
 *
 * @see com.ninchat.master.gson.GsonMetadata
 */
public class EncryptionKey
{
	private static final int BLOCK_SIZE = 16;
	private static final int BLOCK_MASK = BLOCK_SIZE - 1;

	private final String prefix;
	private final SecretKeySpec keySpec;
	private final Base64Encoder encoder;
	private final SecureRandom random;

	public EncryptionKey(String masterKeyId, byte[] masterKeySecretData, Base64Encoder encoder)
	{
		this(masterKeyId, masterKeySecretData, encoder, new SecureRandom());
	}

	public EncryptionKey(String masterKeyId, byte[] masterKeySecretData, Base64Encoder encoder, SecureRandom random)
	{
		prefix = masterKeyId + "-";
		keySpec = new SecretKeySpec(masterKeySecretData, "AES");
		this.encoder = encoder;
		this.random = random;
	}

	/**
	 * This is a low-level interface.  Use only when implementing an
	 * alternative for GsonMetadata.
	 *
	 * @param msg is JSON-encoded text.
	 *
	 * @return a value suitable to be passed to the Ninchat API as the "secure"
	 *         property of a metadata object.
	 */
	public String secureMetadata(byte[] msg) throws BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(msg);
		byte[] hash = md.digest();

		int hashedLen = hash.length + msg.length;
		int paddedLen = (hashedLen + BLOCK_MASK) & ~BLOCK_MASK;
		byte[] padded = Arrays.copyOf(hash, paddedLen);
		System.arraycopy(msg, 0, padded, hash.length, msg.length);

		byte[] iv = new byte[BLOCK_SIZE];
		random.nextBytes(iv);

		Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
		c.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
		byte[] encrypted = c.doFinal(padded);

		int ivEncryptedLen = iv.length + encrypted.length;
		byte[] ivEncrypted = Arrays.copyOf(iv, ivEncryptedLen);
		System.arraycopy(encrypted, 0, ivEncrypted, iv.length, encrypted.length);

		return prefix + encoder.encode(ivEncrypted);
	}
}
