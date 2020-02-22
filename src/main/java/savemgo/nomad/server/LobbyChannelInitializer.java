package savemgo.nomad.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.packet.PacketDecoder;
import savemgo.nomad.packet.PacketEncoder;

public class LobbyChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final PacketEncoder HANDLER_ENCODER = new PacketEncoder();
	private static final PacketDecoder HANDLER_DECODER = new PacketDecoder();

	private final LobbyHandler lobby;
	private final EventExecutorGroup executors;

	public LobbyChannelInitializer(EventExecutorGroup executors, LobbyHandler lobby) {
		this.executors = executors;
		this.lobby = lobby;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("encoder", HANDLER_ENCODER);
		pipeline.addLast("decoder", HANDLER_DECODER);
//		pipeline.addLast("timeout", new ReadTimeoutHandler(60 * 2));
		pipeline.addLast(executors, "lobby", lobby);
	}

}
