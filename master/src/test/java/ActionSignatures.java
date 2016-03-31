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
import com.google.gson.JsonPrimitive;

import com.ninchat.master.SigningKey;
import com.ninchat.master.gson.GsonActionSigning;

class ActionSignatures
{
	static void test(SigningKey key) throws Exception
	{
		long expire = System.currentTimeMillis() / 1000 + 60;
		String userId = "3j63en1k00ri4";
		String channelId = "1bfbr0u";

		JsonArray attr = new JsonArray();
		attr.add(new JsonPrimitive("silenced"));
		attr.add(new JsonPrimitive(false));

		JsonArray memberAttrs = new JsonArray();
		memberAttrs.add(attr);

		Testing.dump(GsonActionSigning.signCreateSession(key, expire));
		Testing.dump(GsonActionSigning.signCreateSessionForUser(key, expire, userId));

		Testing.dump(GsonActionSigning.signJoinChannel(key, expire, channelId));
		Testing.dump(GsonActionSigning.signJoinChannel(key, expire, channelId, memberAttrs));
		Testing.dump(GsonActionSigning.signJoinChannelForUser(key, expire, channelId, userId));
		Testing.dump(GsonActionSigning.signJoinChannelForUser(key, expire, channelId, userId, memberAttrs));
	}
}
