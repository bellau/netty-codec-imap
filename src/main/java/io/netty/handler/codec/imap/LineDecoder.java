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

public class LineDecoder implements ByteBufProcessor {

	public static final byte CR = 13;
	public static final byte LF = 10;
	private static final int MAX_ATOM_LENGTH = 500;

	private final AppendableCharSequence seq = new AppendableCharSequence(128);

	private int size = 0;

	public String parse(ByteBuf buffer) {
		int pos = buffer.forEachByte((ByteBufProcessor) this);
		if (pos == -1) {
			return null;
		} else {
			buffer.readerIndex(pos + 1);
			return new String(seq.toString());
		}
	}

	void reset() {
		seq.reset();
		size = 0;
	}

	@Override
	public boolean process(byte value) throws Exception {
		char nextByte = (char) value;
		if (nextByte == CR) {
			return true;
		} else if (nextByte == LF) {
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
