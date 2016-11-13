package io.netty.handler.codec.imap;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class ImapCommandEncoder extends MessageToMessageEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
		if (msg instanceof ImapCommand) {
			ImapCommand cmd = (ImapCommand) msg;
			ByteBuf buf = ctx.alloc().buffer();
			cmd.write(buf);
			buf.writeByte('\r').writeByte('\n');
			ctx.writeAndFlush(buf);
		}
	}

}
