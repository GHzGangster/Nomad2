package savemgo.nomad.lobby;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.server.LobbyHandler;

@Sharable
public class GateLobby extends LobbyHandler {

	private static final Logger logger = LogManager.getLogger();

	public GateLobby(LocalLobby lobby) {
		super(lobby);
	}

	@Override
	public boolean onPacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Main Lobby */
		case 0x2005:
			Hub.getLobbyList(ctx);
			break;

		case 0x2008:
			Hub.getNewsList(ctx);
			break;

		default:
			logger.printf(Level.DEBUG, "Couldn't handle command %04x", in.getCommand());
			return false;
		}

		return true;
	}

}
