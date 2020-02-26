package savemgo.nomad.lobby;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.helper.Characters;
import savemgo.nomad.helper.Games;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.helper.Users;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.server.LobbyHandler;

@Sharable
public class GameLobby extends LobbyHandler {

	private static final Logger logger = LogManager.getLogger();

	public GameLobby(LocalLobby lobby) {
		super(lobby);
	}

	@Override
	public boolean onPacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Users */
		case 0x3003:
			Users.getSession(ctx, in, false, getLobby());
			break;

		/** Characters */
		case 0x4100:
			// Get Profile Data
			Characters.getCharacterInfo(ctx);
			Characters.getGameplayOptions(ctx);
			Characters.getChatMacros(ctx);
			Characters.getPersonalInfo(ctx);
			Characters.getAvailableGear(ctx);
			Characters.getAvailableSkills(ctx);
			Characters.getSkillSets(ctx);
			Characters.getGearSets(ctx);
			break;

		case 0x4130:
			Characters.setPersonalInfo(ctx, in);
			break;

		case 0x4150:
			Characters.onLobbyDisconnect(ctx, in);
			break;

		case 0x4700:
//			Characters.setConnectionInfo(ctx, in);
			ctx.write(new Packet(0x4701, 0));
			break;

		case 0x4990:
			Characters.getGameEntryInfo(ctx);
			break;

		/** Mail */
		case 0x4820:
//			Messages.getMessages(ctx, in, 0);
			ctx.write(new Packet(0x4821, 0));
			ctx.write(new Packet(0x4823, 0));
			break;

		/** Games */
		case 0x4300:
			Games.getList(ctx, in, getLobby(), 0x4301);
			break;

		case 0x4312:
			Games.getDetails(ctx, in, getLobby());
			break;
		
		case 0x4320:
//			Games.join(ctx, in);
			break;

		/** Hub */
		case 0x4900:
			Hub.getGameLobbyList(ctx);
			break;

		default:
			logger.printf(Level.DEBUG, "Couldn't handle command %04x", in.getCommand());
			return false;
		}

		return true;
	}

}
