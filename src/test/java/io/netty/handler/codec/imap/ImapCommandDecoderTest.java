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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.imap.matcher.ImapCommandMatcher;

public class ImapCommandDecoderTest {

	private EmbeddedChannel channel;

	@Before
	public void before() {
		this.channel = createDecoderChannel();
	}

	@Test
	public void testOnlyCommand() {
		ImapCommand r = testCommand("ZZ01 BLURYBLOOP\r\n");
		assertThat(r, match("ZZ01", "BLURYBLOOP"));

		r = testCommand("ZZ01 BLURYBLOOP\r\n");
		assertThat(r, match("ZZ01", "BLURYBLOOP"));
	}

	@Test
	public void testSplittedCommand() {
		assertThat(testCommand("ZZ01 BLURYBLOO"), nullValue());
		assertThat(testCommand("P\r\n"), match("ZZ01", "BLURYBLOOP"));
	}

	@Test
	public void testCommandAndQuotedStringParam() {
		assertThat(testCommand("ZZ01 BLURYBLOOP \"123\"\r\n"), match("ZZ01", "BLURYBLOOP", "\"123\""));
		assertThat(testCommand("ZZ01 BLURYBLOOP \"123\" \"456\"\r\n"),
				match("ZZ01", "BLURYBLOOP", "\"123\"", "\"456\""));
	}

	@Test
	public void testCommandAndAtomParam() {
		assertThat(testCommand("ZZ01 BLURYBLOOP ABC DCD\r\n"), match("ZZ01", "BLURYBLOOP", "ABC", "DCD"));
	}

	@Test
	public void testCommandAndNumberParam() {
		assertThat(testCommand("ZZ01 BLURYBLOOP 12 34567890\r\n"),
				match("ZZ01", "BLURYBLOOP", new NumberParameter(12), new NumberParameter(34567890)));
	}

	@Test
	public void testCommandAndNilParam() {
		assertThat(testCommand("ZZ01 BLURYBLOOP NIL NUL\r\n"),
				match("ZZ01", "BLURYBLOOP", new NilParameter(), new AtomParameter("NUL")));
	}

	@Test
	public void testCommandAndLiteralParam() {
		assertThat(testCommand("ZZ01 BLURYBLOOP {11+}12345678901 {2+}OK \"OK2\"\r\n"),

				match("ZZ01", "BLURYBLOOP",
						new ChunkParameter(Unpooled.copiedBuffer("12345678901", Charset.defaultCharset()), 0),
						new ChunkParameter(Unpooled.copiedBuffer("OK", Charset.defaultCharset()), 0),
						new QuotedStringParameter("OK2")));
	}

	@Test
	public void testCommandAndListParam() {
		assertThat(testCommand("ZZ01 BLURYBLOOP (ABC DCD NIL)\r\n"),
				match("ZZ01", "BLURYBLOOP", new OpenListParameter(), new AtomParameter("ABC"), new AtomParameter("DCD"),
						new NilParameter(), new CloseListParameter()));

		assertThat(testCommand("ZZ01 BLURYBLOOP (ABC DCD (\"123\" OOO) )\r\n"),
				match("ZZ01", "BLURYBLOOP", new OpenListParameter(), new AtomParameter("ABC"), new AtomParameter("DCD"),
						new OpenListParameter(), new QuotedStringParameter("123"), new AtomParameter("OOO"),
						new CloseListParameter(), new CloseListParameter()));
	}

	@Test
	public void testCommandAndMixedParams() {
		assertThat(testCommand("ZZ01 BLURYBLOOP ABC \"DCD\" OOO\r\n"),
				match("ZZ01", "BLURYBLOOP", "ABC", "\"DCD\"", "OOO"));
	}

	private Matcher<ImapCommand> match(final String tag, final String command) {
		return new ImapCommandMatcher(tag, command, new CommandParameter[0]);
	}

	private Matcher<ImapCommand> match(final String tag, final String command, final String... parameters) {
		return new ImapCommandMatcher(tag, command, Arrays.stream(parameters).map(p -> {
			if (p.startsWith("\"")) {
				return new QuotedStringParameter(p.substring(1, p.length() - 1));
			} else {
				return new AtomParameter(p);
			}
		}).toArray((l) -> {
			return new CommandParameter[l];
		}));
	}

	private Matcher<ImapCommand> match(final String tag, final String command, final CommandParameter... parameters) {
		return new ImapCommandMatcher(tag, command, parameters);
	}

	private ImapCommand testCommand(String command) {

		ByteBuf buffer = Unpooled.wrappedBuffer((command).getBytes());
		if (!channel.writeInbound(buffer)) {
			return null;
		}
		Object o = channel.readInbound();
		assertThat(o, instanceOf(ImapCommand.class));
		ImapCommand request = (ImapCommand) o;
		return request;
	}

	private EmbeddedChannel createDecoderChannel() {
		ImapCommandDecoder decoder = new ImapCommandDecoder();
		EmbeddedChannel channel = new EmbeddedChannel(decoder);
		return channel;
	}
}
