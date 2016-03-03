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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.ninchat.master.EncryptionKey;
import com.ninchat.master.gson.GsonMetadata;

class SecureMetadata
{
	static final String KEY_ID = "3j62l00g00ri4";
	static final String KEY_SECRET = "nBtjEt5iQPW80kvdN9vZl6fqYKb+fSSmCpSgbffYAZM=";

	static void test(EncryptionKey e) throws Exception
	{
		double expire = System.currentTimeMillis() / 1000.0 + 60;
		String userId = "3j63en1k00ri4";

		JsonArray baz = new JsonArray();
		baz.add(new JsonPrimitive(1));
		baz.add(new JsonPrimitive(2));
		baz.add(new JsonPrimitive(3));

		JsonObject quux = new JsonObject();
		quux.addProperty("a", 100);
		quux.addProperty("b", 200);

		JsonObject metadata = new JsonObject();
		metadata.addProperty("foo", 3.14159);
		metadata.addProperty("bar", "asdf");
		metadata.add("baz", baz);
		metadata.add("quux", quux);

		dump(GsonMetadata.secure(e, expire, metadata));
		dump(GsonMetadata.secureForUser(e, expire, metadata, userId));
	}

	static void dump(String s)
	{
		System.out.println("");
		System.out.println("Size: " + Integer.toString(s.length()));
		System.out.println("Data: " + s);
	}
}
