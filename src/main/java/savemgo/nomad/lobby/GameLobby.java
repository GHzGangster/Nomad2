package savemgo.nomad.lobby;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.packet.Packet;

@Sharable
public class GameLobby extends NomadLobby {

	private static final Logger logger = LogManager.getLogger();

	@Override
	public boolean onPacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Main Lobby */
		case 0x2005:
			Hub.getLobbyList(ctx);
			break;

		case 0x2008:
//			Hub.getNews(ctx);
			break;

		default:
			logger.printf(Level.DEBUG, "Couldn't handle command : %04x", in.getCommand());
			return false;
		}

		return true;
	}

}
