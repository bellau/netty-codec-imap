/*
 * Copyright 2016 Laurent Belmonte <laurent.belmonte@gmail.com>
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.imap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class AtomDecoder implements ByteBufProcessor {

	public static class Atom {

		public final String value;
		public final boolean crlf;

		public Atom(String value, boolean crlf) {
			this.value = value;
			this.crlf = crlf;
		}
	}

	public static final byte CR = 13;
	public static final byte LF = 10;
	private static final int MAX_ATOM_LENGTH = 128;
	public static final byte SP = 32;

	private enum Parsed {
		SPACE, CRLF
	}

	private final AppendableCharSequence seq = new AppendableCharSequence(128);

	private Parsed parsed = null;
	private int size = 0;

	public Atom parse(ByteBuf buffer) {
		reset();

		int pos = buffer.forEachByte((ByteBufProcessor) this);
		if (pos == -1) {
			return null;
		} else {
			buffer.readerIndex(pos + 1);
			return new Atom(new String(seq.toString()), parsed == Parsed.CRLF);
		}
	}

	void reset() {
		seq.reset();
		size = 0;
		parsed = null;
	}

	@Override
	public boolean process(byte value) throws Exception {
		char nextByte = (char) value;
		if (nextByte == CR) {
			return true;
		} else if (nextByte == LF) {
			parsed = Parsed.CRLF;
			return false;
		} else if (nextByte == SP) {
			parsed = Parsed.SPACE;
			return false;
		} else {
			if (size >= MAX_ATOM_LENGTH) {
				throw new TooLongFrameException("ATOM is larger than " + MAX_ATOM_LENGTH + " bytes.");
			}
			size++;
			seq.append(nextByte);
			return true;
		}

	}
}
