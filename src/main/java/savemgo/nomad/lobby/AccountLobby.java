package savemgo.nomad.lobby;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.helper.Users;
import savemgo.nomad.packet.Packet;

@Sharable
public class AccountLobby extends NomadLobby {

	private static final Logger logger = LogManager.getLogger();

	@Override
	public boolean onPacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		case 0x3003:
			Users.setSession(ctx, in);
			break;

		default:
			logger.printf(Level.DEBUG, "Couldn't handle command %04x", in.getCommand());
			return false;
		}

		return true;
	}

}
