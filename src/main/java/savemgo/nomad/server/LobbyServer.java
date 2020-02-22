package savemgo.nomad.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.util.Constants;

public class LobbyServer {

	private static final Logger logger = LogManager.getLogger();

	private ServerBootstrap sb;
	private ChannelFuture future;
	private LobbyHandler handler;

	public LobbyServer(EventLoopGroup boss, EventLoopGroup workers, EventExecutorGroup executors,
			LobbyHandler handler) {
		this.handler = handler;

		sb = new ServerBootstrap();
		sb.group(boss, workers);
		sb.channel(NioServerSocketChannel.class);

		sb.option(ChannelOption.SO_BACKLOG, Constants.SERVER_CLIENT_MAX);
		sb.option(ChannelOption.SO_REUSEADDR, true);

		sb.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		sb.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(Constants.SERVER_CLIENT_RCVBUF));
		sb.childOption(ChannelOption.SO_SNDBUF, 65535);
		sb.childOption(ChannelOption.SO_RCVBUF, 65535);

		sb.childHandler(new LobbyChannelInitializer(executors, handler));
		sb.localAddress(handler.getLobby().getIp(), handler.getLobby().getPort());
	}

	public void start() {
		future = sb.bind();
	}

	public void stop() {
		future.cancel(true);
		try {
			future.sync();
		} catch (InterruptedException e) {
			logger.error("Exception occurred while stopping server.", e);
		}
	}

	public ChannelFuture getFuture() {
		return future;
	}

	public LobbyHandler getHandler() {
		return handler;
	}

}
