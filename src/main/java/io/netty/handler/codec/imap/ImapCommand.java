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
import io.netty.buffer.ByteBufUtil;

public class ImapCommand {

	private String tag;
	private List<CommandParameter> parameters;
	private String command;

	public ImapCommand(String tag, String command, List<CommandParameter> parameters) {
		this.tag = tag;
		this.command = command;
		this.parameters = parameters;
	}

	public String getTag() {
		return tag;
	}

	public List<CommandParameter> getParameters() {
		return parameters;
	}

	public String getCommand() {
		return command;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (tag != null) {
			sb.append(tag + " ");
		}

		if (command != null) {
			sb.append(command);
		}

		for (CommandParameter param : parameters) {
			sb.append(" " + param);
		}

		return sb.toString();
	}

	public boolean partial() {
		if (parameters.size() == 0) {
			return false;
		} else {
			return getLastParameter().isPartial();
		}
	}

	public CommandParameter getLastParameter() {
		if (parameters.size() == 0) {
			return null;
		}
		return parameters.get(parameters.size() - 1);
	}

	public void write(ByteBuf buf) {
		ByteBufUtil.writeAscii(buf, tag);
		buf.writeByte(' ');
		ByteBufUtil.writeAscii(buf, command);
		if (parameters != null && !parameters.isEmpty()) {
			CommandParameter.write(buf, parameters);
		}
	}

}
