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

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.imap.AtomDecoder.Atom;

public class ImapCommandDecoder extends ByteToMessageDecoder {

	private enum State {
		READ_TAG,

		READ_COMMAND, READ_PARAMTERS
	}

	private State currentState;
	private AtomDecoder atomDecoder = new AtomDecoder();
	private ImapCommandBuilder builder;
	private ParameterDecoder paramDecoder = new ParameterDecoder();

	public ImapCommandDecoder() {
		resetNow();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		switch (currentState) {
		case READ_TAG: {
			Atom atom = atomDecoder.parse(in);
			if (atom == null) {
				return;
			}
			if (atom.crlf) {
				// FIXME throw
				throw new Exception("not allowed");
			}

			builder.tag(atom.value);
			currentState = State.READ_COMMAND;
		}
		case READ_COMMAND: {
			Atom atom = atomDecoder.parse(in);
			if (atom == null) {
				return;
			}
			builder.command(atom.value);
			if (atom.crlf) {
				ImapCommand r = builder.build();
				out.add(r);
				resetNow();
				break;
			} else {
				currentState = State.READ_PARAMTERS;
			}
		}
		case READ_PARAMTERS: {
			CommandParameter param = null;
			while ((param = paramDecoder.next(ctx, in)) != null) {
				builder.addParam(param);
			}

			if (paramDecoder.getState() == ParameterDecoder.State.Ended) {
				ImapCommand r = builder.build();
				out.add(r);
				resetNow();
				break;
			}
		}
		}
	}

	private void resetNow() {
		builder = new ImapCommandBuilder();
		atomDecoder.reset();
		currentState = State.READ_TAG;
		paramDecoder.reset();
	}

}
