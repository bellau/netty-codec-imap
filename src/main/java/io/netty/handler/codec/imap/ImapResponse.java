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

import java.util.Arrays;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public interface ImapResponse {

	public class ResponseCode {
		public ResponseCode(String statusCode, List<CommandParameter> statusCodeParams) {
			name = statusCode;
			this.parameters = statusCodeParams;
		}

		public String name;
		public List<CommandParameter> parameters;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResponseCode other = (ResponseCode) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (parameters == null) {
				if (other.parameters != null)
					return false;
			} else if (!parameters.equals(other.parameters))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ResponseCode [name=" + name + ", parameters=" + parameters + "]";
		}

		public void write(ByteBuf buf) {
			buf.writeByte('[');
			ByteBufUtil.writeAscii(buf, name);
			CommandParameter.write(buf, parameters);
			buf.writeByte(']');
		}

	}

	public interface StatusResponse extends ImapResponse {
		public ResponseCode getCode();

		public static StatusResponse create(String tag, String statusCode, List<CommandParameter> statusCodeParams,
				String statusResponse, String statusReponseMessage) {
			ResponseCode code = statusCode != null ? new ResponseCode(statusCode, statusCodeParams) : null;
			if (statusResponse.equals("OK")) {
				return new Ok(tag, code, statusReponseMessage);
			} else if (statusResponse.equals("BYE")) {
				return new ByeResponse(code, statusReponseMessage);
			} else if (statusResponse.equals("BAD")) {
				return new Bad(tag, code, statusReponseMessage);
			} else if (statusResponse.equals("NO")) {
				return new No(tag, code, statusReponseMessage);
			} else if (statusResponse.equals("PREAUTH")) {
				return new PreAuthResponse(code, statusReponseMessage);
			} else {
				// FIXME
				return null;
			}
		}
	}

	public class ServerResponse implements ImapResponse {

		public ServerResponse(String command, List<CommandParameter> parameters) {
			this.command = command;
			this.parameters = parameters;
		}

		public final String command;
		public final List<CommandParameter> parameters;

		@Override
		public boolean tagged() {
			return false;
		}

		@Override
		public List<CommandParameter> getParameters() {
			return parameters;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((command == null) ? 0 : command.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServerResponse other = (ServerResponse) obj;
			if (command == null) {
				if (other.command != null)
					return false;
			} else if (!command.equals(other.command))
				return false;
			if (parameters == null) {
				if (other.parameters != null)
					return false;
			} else if (!parameters.equals(other.parameters))
				return false;
			return true;
		}

		@Override
		public void write(ByteBuf buf) {
			buf.writeByte('*');
			buf.writeByte(' ');
			ByteBufUtil.writeAscii(buf, command);
			CommandParameter.write(buf, parameters);
		}

		@Override
		public String toString() {
			return "ServerResponse [command=" + command + ", parameters=" + parameters + "]";
		}

	}

	public static class GenericReponse implements StatusResponse {
		public final String tag;
		public final ResponseCode code;
		private final String reponse;
		public final List<CommandParameter> parameters;

		public GenericReponse(String tag, ResponseCode code, String response, String message) {
			this.tag = tag;
			this.code = code;
			this.reponse = response;
			this.parameters = Arrays.asList(new HumanReadableParameter(message));
		}

		@Override
		public boolean tagged() {
			return tag != null;
		}

		@Override
		public List<CommandParameter> getParameters() {
			return parameters;
		}

		@Override
		public ResponseCode getCode() {
			return code;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((code == null) ? 0 : code.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GenericReponse other = (GenericReponse) obj;
			if (code == null) {
				if (other.code != null)
					return false;
			} else if (!code.equals(other.code))
				return false;
			if (parameters == null) {
				if (other.parameters != null)
					return false;
			} else if (!parameters.equals(other.parameters))
				return false;
			if (tag == null) {
				if (other.tag != null)
					return false;
			} else if (!tag.equals(other.tag))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "GenericReponse [tag=" + tag + ", code=" + code + ", reponse=" + reponse + ", parameters="
					+ parameters + "]";
		}

		@Override
		public void write(ByteBuf buf) {
			if (tag != null) {
				ByteBufUtil.writeAscii(buf, tag);

			} else {
				buf.writeByte('*');
			}
			buf.writeByte(' ');
			ByteBufUtil.writeAscii(buf, reponse);

			if (code != null) {
				buf.writeByte(' ');
				code.write(buf);
			}

			if (parameters != null) {
				CommandParameter.write(buf, parameters);
			}
		}

	}

	public static class Ok extends GenericReponse {

		public Ok(String tag, ResponseCode code, String message) {
			super(tag, code, "OK", message);
		}

	}

	public static class No extends GenericReponse {

		public No(String tag, ResponseCode code, String message) {
			super(tag, code, "NO", message);
		}

	}

	public static class MessageStatusResponse implements ImapResponse {

		@Override
		public String toString() {
			return "MessageStatusResponse [command=" + command + ", number=" + number + "]";
		}

		public final String command;
		public final int number;
		private List<CommandParameter> parameters;

		public MessageStatusResponse(int number, String command, List<CommandParameter> parameters) {
			this.number = number;
			this.command = command;
			this.parameters = parameters;
		}

		@Override
		public boolean tagged() {
			return false;
		}

		@Override
		public List<CommandParameter> getParameters() {
			return parameters;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((command == null) ? 0 : command.hashCode());
			result = prime * result + number;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MessageStatusResponse other = (MessageStatusResponse) obj;
			if (command == null) {
				if (other.command != null)
					return false;
			} else if (!command.equals(other.command))
				return false;
			if (number != other.number)
				return false;
			return true;
		}

		@Override
		public void write(ByteBuf buf) {
			buf.writeByte('*');
			buf.writeByte(' ');
			ByteBufUtil.writeAscii(buf, Integer.toString(number));

			buf.writeByte(' ');
			ByteBufUtil.writeAscii(buf, command);

			if (parameters != null) {
				CommandParameter.write(buf, parameters);
			}
		}

	}

	public static class ByeResponse extends GenericReponse {

		public ByeResponse(ResponseCode code, String message) {
			super(null, code, "BYE", message);
		}
	}

	public static class Bad extends GenericReponse {

		public Bad(String tag, ResponseCode code, String message) {
			super(tag, code, "BAD", message);
		}
	}

	public static class PreAuthResponse extends GenericReponse {

		public PreAuthResponse(ResponseCode code, String message) {
			super(null, code, "PREAUTH", message);
		}

	}

	public boolean tagged();

	public List<CommandParameter> getParameters();

	public void write(ByteBuf buf);
}
