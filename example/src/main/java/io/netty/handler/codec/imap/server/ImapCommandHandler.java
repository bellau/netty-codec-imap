package io.netty.handler.codec.imap.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.imap.ImapCommand;
import io.netty.handler.codec.imap.client.ImapClient;

public class ImapCommandHandler extends SimpleChannelInboundHandler<ImapCommand> {

	private ImapClient client;

	public ImapCommandHandler(ImapClient client) {
		this.client = client;

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ImapCommand cmd) throws Exception {
		client.writeCommand(cmd);
	}
}
