package savemgo.nomad.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Buffers;

public abstract class LobbyHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger();

	private LocalLobby lobby;

	public LobbyHandler(LocalLobby lobby) {
		this.lobby = lobby;
	}

	public abstract boolean onPacket(ChannelHandlerContext ctx, Packet ins);

	public void onConnectionOpened(ChannelHandlerContext ctx) {

	}

	public void onConnectionClosed(ChannelHandlerContext ctx) {

	}

	private final int onPacketCommon(ChannelHandlerContext ctx, Packet in) {
		var command = in.getCommand();

		switch (command) {

		case 0x0003:
			ctx.close();
			return 0;

		case 0x0005:
			ctx.write(new Packet(0x0005));
			break;

		default:
			return -1;
		}

		return 1;
	}

	@Override
	public final void channelRead(ChannelHandlerContext ctx, Object msg) {
		Packet in = (Packet) msg;

		boolean wrote = false;
		try {
			int result = onPacketCommon(ctx, in);
			if (result == 1) {
				wrote = true;
			} else if (result < 0) {
				wrote = onPacket(ctx, in);
			}
		} catch (Exception e) {
			logger.error("Exception while handling packet.", e);
		} finally {
			Buffers.release(in.getPayload());
		}

		if (wrote) {
			ctx.flush();
		}
	}

	@Override
	public final void channelActive(ChannelHandlerContext ctx) {
		logger.debug("Connection opened.");
		onConnectionOpened(ctx);
	}

	@Override
	public final void channelInactive(ChannelHandlerContext ctx) {
		logger.debug("Connection closed.");
		onConnectionClosed(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof ReadTimeoutException) {
			logger.debug("Connection timed out.");
			channelInactive(ctx);
		} else {
			logger.debug("Exception caught.", cause);
		}
	}

	public LocalLobby getLobby() {
		return lobby;
	}

}
