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

public interface CommandParameter {

	public boolean isPartial();

	public static void write(ByteBuf buf, List<CommandParameter> parameters) {
		if (parameters == null) {
			return;
		}
		CommandParameter last = null;
		for (CommandParameter p : parameters) {
			if (last == null || (!(last instanceof OpenListParameter) && !(p instanceof CloseListParameter)
					&& !(p instanceof ChunkParameter))) {
				buf.writeByte(' ');
			}
			last = p;
			p.write(buf);
		}
	}

	public void write(ByteBuf buf);
}
