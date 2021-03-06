package savemgo.nomad.lobby;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.helper.Users;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.server.LobbyHandler;

@Sharable
public class AccountLobby extends LobbyHandler {

	private static final Logger logger = LogManager.getLogger();

	public AccountLobby(LocalLobby lobby) {
		super(lobby);
	}

	@Override
	public boolean onPacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Users */
		case 0x3003:
			Users.getSession(ctx, in, getLobby());
			break;

		case 0x3048:
			Users.getCharacterList(ctx);
			break;

		case 0x3101:
			Users.createCharacter(ctx, in);
			break;

		case 0x3103:
			Users.selectCharacter(ctx, in);
			break;

		case 0x3105:
			Users.deleteCharacter(ctx, in);
			break;

		default:
			logger.printf(Level.DEBUG, "Couldn't handle command %04x", in.getCommand());
			return false;
		}

		return true;
	}

	@Override
	public void onConnectionClosed(ChannelHandlerContext ctx) {
		Users.onLobbyLeave(ctx.channel());
	}

}
