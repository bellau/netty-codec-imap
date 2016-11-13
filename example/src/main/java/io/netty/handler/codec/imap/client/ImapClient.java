package io.netty.handler.codec.imap.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.imap.ImapCommand;
import io.netty.handler.codec.imap.ImapCommandEncoder;
import io.netty.handler.codec.imap.ImapResponse;
import io.netty.handler.codec.imap.ImapResponseDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ImapClient {

	private Bootstrap bootstrap;
	private ChannelHandlerContext ctx;

	public ImapClient(Bootstrap bootstrap) {
		this.bootstrap = bootstrap;
	}

	ChannelHandler initializer() {
		return new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
				pipeline.addLast(new ImapResponseDecoder());
				pipeline.addLast(new ImapCommandEncoder());
				pipeline.addLast(responseHandler());
			}
		};
	}

	public interface ResponseHandler {
		public void handle(ImapResponse response);
	}

	private ResponseHandler handler;

	protected ChannelHandler responseHandler() {
		return new SimpleChannelInboundHandler<ImapResponse>() {

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
				cause.printStackTrace();
				ctx.close();
			}

			@Override
			protected void channelRead0(ChannelHandlerContext ctx, ImapResponse response) throws Exception {
				handlerResponse(response);
			}

			@Override
			public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
				super.channelRegistered(ctx);
				ImapClient.this.ctx = ctx;
			}

		};
	}

	public void responseHandler(ResponseHandler handler) {
		this.handler = handler;
	}

	protected void handlerResponse(ImapResponse response) {
		handler.handle(response);
	}

	public void connect(String host, int port) {
		bootstrap.handler(initializer());
		bootstrap.connect(host, port);
	}

	public void writeCommand(ImapCommand cmd) {
		ctx.writeAndFlush(cmd);
	}
}
