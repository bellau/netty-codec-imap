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

public class NumberParameter implements CommandParameter {

	private final int value;

	public NumberParameter(int value) {
		this.value = value;
	}

	@Override
	public boolean isPartial() {
		return false;
	}

	public int getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
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
		NumberParameter other = (NumberParameter) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public void write(ByteBuf buf) {
		ByteBufUtil.writeAscii(buf, Integer.toString(value));
	}

}
