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
import io.netty.buffer.Unpooled;

public class ChunkParameter implements CommandParameter {

	private ByteBuf buffer;
	private boolean last;

	public ChunkParameter(ByteBuf read, boolean last) {
		this.buffer = read;
		this.last = last;
	}

	@Override
	public String toString() {
		ByteBuf buf = Unpooled.buffer();
		write(buf);
		return buf.toString(Charset.defaultCharset());
	}

	@Override
	public boolean isPartial() {
		return last;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buffer == null) ? 0 : buffer.hashCode());
		result = prime * result + (last ? 1231 : 1237);
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
		ChunkParameter other = (ChunkParameter) obj;
		if (buffer == null) {
			if (other.buffer != null)
				return false;
		} else if (!buffer.equals(other.buffer))
			return false;
		if (last != other.last)
			return false;
		return true;
	}

	@Override
	public void write(ByteBuf buf) {
		buf.writeBytes(buffer.slice());
	}

}
