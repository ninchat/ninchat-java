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

import java.util.Comparator;

/**
 * @author Kari Lavikka
 */
public class ChatComparator implements Comparator<Conversation> {
	@Override
	public int compare(Conversation a, Conversation b) {
		if (a instanceof Dialogue && b instanceof Dialogue) {
			return a.getName().compareToIgnoreCase(b.getName());

		} else if (a instanceof Channel && b instanceof Channel) {
			Channel ac = (Channel)a;
			Channel bc = (Channel)b;

			if (ac.getRealm() == bc.getRealm()) {
				return ac.getName().compareToIgnoreCase(bc.getName());

			} else if (ac.getRealm() == null) {
				return -1;

			} else if (bc.getRealm() == null) {
				return 1;

			} else {
				return ac.getRealm().getName().compareToIgnoreCase(bc.getRealm().getName());
			}

		} else if (a instanceof Channel) {
			return -1;

		} else {
			assert b instanceof Channel;
			return 1;
		}
	}
}
