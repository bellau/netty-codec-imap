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

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class LiteralParameter implements CommandParameter {

	private int total;
	private ByteBuf buffer;
	private boolean plus;

	public LiteralParameter(ByteBuf read, int total, boolean plus) {
		this.buffer = read;
		this.total = total;
		this.plus = plus;
	}

	@Override
	public String toString() {
		ByteBuf buf = Unpooled.buffer();
		write(buf);
		return buf.toString(Charset.defaultCharset());
	}

	@Override
	public boolean isPartial() {
		return total != buffer.readableBytes();
	}

	@Override
	public void write(ByteBuf buf) {
		buf.writeByte('{');
		ByteBufUtil.writeAscii(buf, Integer.toString(total));
		if (plus) {
			buf.writeByte('+');
		}
		buf.writeByte('}');
		buf.writeByte('\r');
		buf.writeByte('\n');
		buf.writeBytes(buffer.slice());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buffer == null) ? 0 : buffer.hashCode());
		result = prime * result + (plus ? 1231 : 1237);
		result = prime * result + total;
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
		LiteralParameter other = (LiteralParameter) obj;
		if (buffer == null) {
			if (other.buffer != null)
				return false;
		} else if (!buffer.equals(other.buffer))
			return false;
		if (plus != other.plus)
			return false;
		if (total != other.total)
			return false;
		return true;
	}

}
