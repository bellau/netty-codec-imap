package io.netty.handler.codec.imap.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class SimpleImapClientFactory {
	private EventLoopGroup group = new NioEventLoopGroup();

	public ImapClient client() throws InterruptedException {
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class);
		b.handler(new LoggingHandler(LogLevel.INFO));

		ImapClient client = new ImapClient(b);

		return client;
	}

}
