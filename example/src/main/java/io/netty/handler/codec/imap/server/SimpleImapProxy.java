package io.netty.handler.codec.imap.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.imap.ImapCommandDecoder;
import io.netty.handler.codec.imap.ImapResponse;
import io.netty.handler.codec.imap.ImapResponseEncoder;
import io.netty.handler.codec.imap.client.ImapClient;
import io.netty.handler.codec.imap.client.SimpleImapClientFactory;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class SimpleImapProxy {

	class Init extends ChannelInitializer<SocketChannel> {

		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			ImapClient client = imapClient.client();

			ChannelPipeline pipeline = ch.pipeline();

			// pipeline.addLast(new SslHandler(engine));
			pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
			pipeline.addLast(new ImapCommandDecoder());
			pipeline.addLast(new ImapResponseEncoder());
			pipeline.addLast(new ImapCommandHandler(client));

			client.responseHandler(new ImapClient.ResponseHandler() {

				@Override
				public void handle(ImapResponse response) {
					ch.writeAndFlush(response);
				}
			});
			client.connect(remoteHost, remotePort);
		}
	}

	private SimpleImapClientFactory imapClient = new SimpleImapClientFactory();
	private int remotePort;
	private String remoteHost;
	private int port;

	public SimpleImapProxy(int port, String remoteHost, int remotePort) {
		this.port = port;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	public void start() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO)).childHandler(new Init());

			b.bind(port).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {

		String remoteHost = "localhost";
		int remotePort = 143;
		if (args.length == 3) {
			remoteHost = args[1];
			remotePort = Integer.parseInt(args[2]);
		}
		new SimpleImapProxy(1143, remoteHost, remotePort).start();
	}
}
