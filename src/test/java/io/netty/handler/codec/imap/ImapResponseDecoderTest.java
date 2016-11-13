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
import static org.junit.Assert.assertThat;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.imap.ImapResponse.ResponseCode;

public class ImapResponseDecoderTest {

	private EmbeddedChannel channel;

	@Before
	public void before() {
		this.channel = createDecoderChannel();
	}

	@Test
	public void testTaggedOkResponse() {
		ImapResponse r = testResponse("A001 OK LOGIN Completed\r\n");
		assertThat(r, match(new ImapResponse.Ok("A001", null, "LOGIN Completed")));

		r = testResponse("A001 OK [ALERT] LOGIN Completed\r\n");
		assertThat(r, match(new ImapResponse.Ok("A001", new ResponseCode("ALERT", null), "LOGIN Completed")));
		assertThat(testResponse("C00047 OK [READ-WRITE] Complete\r\n"),
				match(new ImapResponse.Ok("C00047", new ResponseCode("READ-WRITE", null), "Complete")));
	}

	@Test
	public void testUntaggedOkResponse() {
		ImapResponse r = testResponse("* OK [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited\r\n");
		assertThat(r,
				match(new ImapResponse.Ok(null, new ResponseCode("PERMANENTFLAGS",
						Arrays.asList(new OpenListParameter(), new AtomParameter("\\Deleted"),
								new AtomParameter("\\Seen"), new AtomParameter("\\*"), new CloseListParameter())),
						"Limited")));

		r = testResponse("T00083 OK Completed (0.000 sec)\r\n");
		assertThat(r, match(new ImapResponse.Ok("T00083", null, "Completed (0.000 sec)")));

	}

	@Test
	public void testUntaggedOkResponse2() {
		ImapResponse r = testResponse(
				"* OK [CAPABILITY IMAP4rev1 LITERAL+ ID ENABLE STARTTLS AUTH=PLAIN SASL-IR] server ready\r\n");
		assertThat(r,
				match(new ImapResponse.Ok(null, new ResponseCode("CAPABILITY",
						Arrays.asList(new AtomParameter("IMAP4rev1"), new AtomParameter("LITERAL+"),
								new AtomParameter("ID"), new AtomParameter("ENABLE"), new AtomParameter("STARTTLS"),
								new AtomParameter("AUTH=PLAIN"), new AtomParameter("SASL-IR"))),
						"server ready")));

	}

	@Test
	public void testTaggedNoResponse() {
		ImapResponse r = testResponse("A001 NO LOGIN Completed\r\n");
		assertThat(r, match(new ImapResponse.No("A001", null, "LOGIN Completed")));

		r = testResponse("A001 NO [ALERT] LOGIN Completed\r\n");
		assertThat(r, match(new ImapResponse.No("A001", new ResponseCode("ALERT", null), "LOGIN Completed")));
	}

	@Test
	public void testUntaggedNoResponse() {
		ImapResponse r = testResponse("* NO [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited\r\n");
		assertThat(r,
				match(new ImapResponse.No(null, new ResponseCode("PERMANENTFLAGS",
						Arrays.asList(new OpenListParameter(), new AtomParameter("\\Deleted"),
								new AtomParameter("\\Seen"), new AtomParameter("\\*"), new CloseListParameter())),
						"Limited")));
	}

	@Test
	public void testTaggedBadResponse() {
		ImapResponse r = testResponse("A001 BAD LOGIN Completed\r\n");
		assertThat(r, match(new ImapResponse.Bad("A001", null, "LOGIN Completed")));

		r = testResponse("A001 BAD [ALERT] LOGIN Completed\r\n");
		assertThat(r, match(new ImapResponse.Bad("A001", new ResponseCode("ALERT", null), "LOGIN Completed")));
	}

	@Test
	public void testUntaggedBadResponse() {
		ImapResponse r = testResponse("* BAD [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited\r\n");
		assertThat(r,
				match(new ImapResponse.Bad(null, new ResponseCode("PERMANENTFLAGS",
						Arrays.asList(new OpenListParameter(), new AtomParameter("\\Deleted"),
								new AtomParameter("\\Seen"), new AtomParameter("\\*"), new CloseListParameter())),
						"Limited")));
	}

	@Test
	public void testPreauthResponse() {
		ImapResponse r = testResponse("* PREAUTH [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited\r\n");
		assertThat(r, match(new ImapResponse.PreAuthResponse(
				new ResponseCode("PERMANENTFLAGS",
						Arrays.asList(new OpenListParameter(), new AtomParameter("\\Deleted"),
								new AtomParameter("\\Seen"), new AtomParameter("\\*"), new CloseListParameter())),
				"Limited")));
	}

	@Test
	public void testByeResponse() {
		ImapResponse r = testResponse("* BYE [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited\r\n");
		assertThat(r, match(new ImapResponse.ByeResponse(
				new ResponseCode("PERMANENTFLAGS",
						Arrays.asList(new OpenListParameter(), new AtomParameter("\\Deleted"),
								new AtomParameter("\\Seen"), new AtomParameter("\\*"), new CloseListParameter())),
				"Limited")));
	}

	@Test
	public void testStatusResponse() {
		ImapResponse r = testResponse("* 5 RECENT\r\n");
		assertThat(r, match(new ImapResponse.MessageStatusResponse(5, "RECENT", null)));

		r = testResponse("* 12 FETCH (FLAGS (\\Seen $has_cal) UID 304)\r\n");
	}

	@Test
	public void testUntaggedCommandResponse() {
		ImapResponse r = testResponse("* CAPABILITY IMAP4rev1 STARTTLS AUTH=GSSAPI LOGINDISABLED\r\n");
		assertThat(r, match(new ImapResponse.ServerResponse("CAPABILITY", Arrays.asList(new AtomParameter("IMAP4rev1"),
				new AtomParameter("STARTTLS"), new AtomParameter("AUTH=GSSAPI"), new AtomParameter("LOGINDISABLED")))));
	}

	@Test
	public void testCommandResponse() {
		ImapResponse r = testResponse(
				"* NAMESPACE ( ( \"\" \"/\" ) ) ( ( \"Autres utilisateurs/\" \"/\" ) ) ( ( \"Dossiers partag&AOk-s/\" \"/\" ) )\r\n");

		assertThat(r,
				match(new ImapResponse.ServerResponse("NAMESPACE",
						Arrays.asList(new OpenListParameter(), new OpenListParameter(), new QuotedStringParameter(""),
								new QuotedStringParameter("/"), new CloseListParameter(), new CloseListParameter(),
								new OpenListParameter(), new OpenListParameter(),
								new QuotedStringParameter("Autres utilisateurs/"), new QuotedStringParameter("/"),
								new CloseListParameter(), new CloseListParameter(), new OpenListParameter(),
								new OpenListParameter(), new QuotedStringParameter("Dossiers partag&AOk-s/"),
								new QuotedStringParameter("/"), new CloseListParameter(), new CloseListParameter()))));
		assertThat(r,
				match(new ImapResponse.ServerResponse("NAMESPACE",
						Arrays.asList(new OpenListParameter(), new OpenListParameter(), new QuotedStringParameter(""),
								new QuotedStringParameter("/"), new CloseListParameter(), new CloseListParameter(),
								new OpenListParameter(), new OpenListParameter(),
								new QuotedStringParameter("Autres utilisateurs/"), new QuotedStringParameter("/"),
								new CloseListParameter(), new CloseListParameter(), new OpenListParameter(),
								new OpenListParameter(), new QuotedStringParameter("Dossiers partag&AOk-s/"),
								new QuotedStringParameter("/"), new CloseListParameter(), new CloseListParameter()))));

	}

	private Matcher<ImapResponse> match(ImapResponse response) {
		return CoreMatchers.equalTo(response);
	}

	private ImapResponse testResponse(String command) {

		ByteBuf buffer = Unpooled.wrappedBuffer((command).getBytes());
		if (!channel.writeInbound(buffer)) {
			return null;
		}
		Object o = channel.readInbound();
		assertThat(o, instanceOf(ImapResponse.class));
		ImapResponse response = (ImapResponse) o;
		ByteBuf buff = Unpooled.buffer();
		response.write(buff);
		System.err.println(buff.toString(Charset.defaultCharset()));
		return response;
	}

	private EmbeddedChannel createDecoderChannel() {
		ImapResponseDecoder decoder = new ImapResponseDecoder();
		EmbeddedChannel channel = new EmbeddedChannel(decoder);
		return channel;
	}
}
