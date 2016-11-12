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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class ParameterDecoder {

	private static final byte QUOTE = '"';
	private static final byte OPEN_BRACKET = '{';
	private static final byte CLOSE_BRACKET = '}';
	private static final byte OPEN_PARENTHESES = '(';
	private static final byte CLOSE_PARENTHESES = ')';
	public static final byte CR = 13;
	public static final byte LF = 10;
	public static final byte SP = 32;

	private static final int MAX_ATOM_LENGTH = 128;

	public static class LiteralLength {
		public LiteralLength(int parseInt, boolean b) {
			this.length = parseInt;
			r = parseInt;
			this.plus = b;
		}

		boolean plus;
		int length;

		private int r;

		public int remainingLength() {
			return r;
		}

		public void read(int toRead) {
			r = r - toRead;
		}
	}

	public enum State {
		EMPTY, READ_QUOTED_STRING, READ_LITERAL_LENGTH, READ_PARAM_LIST, READ_ATOM, END_PARAM_LIST, NEXT, Ended, READ_LITERAL
	}

	private final AppendableCharSequence seq = new AppendableCharSequence(128);

	private int size = 0;

	private State currentState = State.NEXT;
	private LiteralLength literalLength;

	public State getState() {
		return currentState;
	}

	public CommandParameter next(ChannelHandlerContext ctx, ByteBuf in) {

		if (in.readableBytes() == 0) {
			return null;
		}

		byte firstByte = in.getByte(in.readerIndex());
		int i = in.readableBytes();
		byte[] dst = new byte[i];
		in.getBytes(in.readerIndex(), dst);
		switch (currentState) {
		case NEXT: {
			switch (firstByte) {
			case SP:
				currentState = State.EMPTY;
				in.skipBytes(1);
				return next(ctx, in);
			case CR:
				in.skipBytes(1);
				return next(ctx, in);
			case LF:
				in.skipBytes(1);
				currentState = State.Ended;
				return null;
			case CLOSE_PARENTHESES:
				currentState = State.EMPTY;
				break;
			default:
				throw new CorruptedFrameException("byte " + new Character((char) firstByte));

			}
		}

		case EMPTY:
			switch (firstByte) {
			case QUOTE:
				currentState = State.READ_QUOTED_STRING;
				break;
			case OPEN_BRACKET:
				currentState = State.READ_LITERAL_LENGTH;
				break;
			case OPEN_PARENTHESES:
				currentState = State.READ_PARAM_LIST;
				break;
			case CLOSE_PARENTHESES:
				currentState = State.END_PARAM_LIST;
				break;
			default:
				currentState = State.READ_ATOM;
			}
		default:
		}

		switch (currentState) {
		case EMPTY:
			return null;
		case READ_ATOM: {
			CommandParameter atom = decodeAtom(in);
			if (atom != null) {
				currentState = State.NEXT;
			}
			return atom;
		}
		case READ_QUOTED_STRING: {
			CommandParameter ret = decodeQuotedString(in);

			if (ret != null) {
				currentState = State.NEXT;
			}
			return ret;
		}
		case READ_PARAM_LIST: {
			currentState = State.EMPTY;
			in.skipBytes(1);
			return new OpenListParameter();
		}
		case END_PARAM_LIST: {
			currentState = State.NEXT;
			in.skipBytes(1);
			return new CloseListParameter();
		}
		case READ_LITERAL_LENGTH: {
			literalLength = decodeLiteralLength(in);
			if (literalLength != null) {
				in.skipBytes(1);
				currentState = State.READ_LITERAL;
			} else {
				return null;
			}
		}
		case READ_LITERAL: {
			CommandParameter literalCommand = decodeLiteral(ctx, in);
			if (literalLength == null) {
				currentState = State.NEXT;
			}

			return literalCommand;
		}
		default:
		}
		return null;
	}

	private CommandParameter decodeLiteral(ChannelHandlerContext ctx, ByteBuf in) {
		int toRead = Math.min(literalLength.remainingLength(), in.readableBytes());
		if (toRead == 0) {
			return null;
		}

		ByteBuf read = ByteBufUtil.readBytes(ctx.alloc(), in, toRead);
		literalLength.read(toRead);

		ChunkParameter ret = new ChunkParameter(read, literalLength.remainingLength());
		if (literalLength.remainingLength() == 0) {
			literalLength = null;
		}

		return ret;
	}

	private LiteralLength decodeLiteralLength(ByteBuf in) {
		in.skipBytes(1);
		int pos = in.forEachByte((value) -> {

			char nextByte = (char) value;
			if ((nextByte >= '0' && nextByte <= '9') || nextByte == '+') {
				if (size >= MAX_ATOM_LENGTH) {
					throw new TooLongFrameException("ATOM is larger than " + MAX_ATOM_LENGTH + " bytes.");
				}
				size++;
				seq.append(nextByte);
				return true;
			} else if (nextByte == CLOSE_BRACKET) {
				return false;
			} else {
				throw new CorruptedFrameException();
			}
		});

		if (pos == -1) {
			return null;
		} else {
			in.readerIndex(pos);
			String v = new String(seq.toString());
			LiteralLength ret = null;
			if (v.charAt(v.length() - 1) == '+') {
				ret = new LiteralLength(Integer.parseInt(v.substring(0, v.length() - 1)), true);
			} else {
				ret = new LiteralLength(Integer.parseInt(v), true);
			}
			seq.reset();
			size = 0;
			return ret;
		}
	}

	private CommandParameter decodeAtom(ByteBuf in) {
		int pos = in.forEachByte((value) -> {

			char nextByte = (char) value;
			if (nextByte == LF || nextByte == CR || nextByte == SP || nextByte == CLOSE_PARENTHESES) {
				return false;
			} else {
				if (size >= MAX_ATOM_LENGTH) {
					throw new TooLongFrameException("ATOM is larger than " + MAX_ATOM_LENGTH + " bytes.");
				}
				size++;
				seq.append(nextByte);
				return true;
			}
		});

		if (pos == -1) {
			return null;
		} else {
			in.readerIndex(pos);
			String value = new String(seq.toString());
			CommandParameter ret = null;
			char firstChar = value.charAt(0);
			if (firstChar >= '0' && firstChar <= '9') {
				ret = new NumberParameter(Integer.parseInt(value));
			} else if (firstChar == 'N' && value.equals("NIL")) {
				ret = new NilParameter();
			} else {
				ret = new AtomParameter(value);
			}
			seq.reset();
			size = 0;
			return ret;
		}
	}

	private CommandParameter decodeQuotedString(ByteBuf in) {
		in.skipBytes(1);
		int pos = in.forEachByte((value) -> {

			char nextByte = (char) value;
			if (nextByte == QUOTE) {
				return false;
			} else if (nextByte == LF || nextByte == CR) {
				throw new CorruptedFrameException();
			} else {
				if (size >= MAX_ATOM_LENGTH) {
					throw new TooLongFrameException("ATOM is larger than " + MAX_ATOM_LENGTH + " bytes.");
				}
				size++;
				seq.append(nextByte);
				return true;
			}
		});

		if (pos == -1) {
			return null;
		} else {
			in.readerIndex(pos + 1);
			CommandParameter ret = new QuotedStringParameter(new String(seq.toString()));
			seq.reset();
			size = 0;
			return ret;
		}
	}

	public void reset() {
		seq.reset();
		size = 0;
		currentState = State.EMPTY;
	}

}
