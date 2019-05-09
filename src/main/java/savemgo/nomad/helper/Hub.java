package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.Nomad;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.util.ByteBufEx;
import savemgo.nomad.util.Util;

public class Hub {

	private static final Logger logger = LogManager.getLogger();

	public static void getLobbyList(ChannelHandlerContext ctx) {
		PayloadGroup payloads = null;
		try {
			var lobbies = Nomad.get().getLobbies();
			var iterator = lobbies.iterator();

			payloads = Util.createPayloads(lobbies.size(), 0x2e, 22, (bo, i) -> {
				NomadLobby lobby = iterator.next();

				boolean beginner = false, expansion = false, noHeadshot = false;

				int restriction = 0;
				restriction |= beginner ? 0b1 : 0;
				restriction |= expansion ? 0b1000 : 0;
				restriction |= noHeadshot ? 0b10000 : 0;

				var box = new ByteBufEx(bo);
				
				bo.writeInt(i).writeInt(lobby.getType());
				box.writeStringFill(lobby.getName(), 16).writeStringFill(lobby.getIp(), 15);
				bo.writeShort(lobby.getPort()).writeShort(lobby.getPlayers()).writeShort(lobby.getId())
						.writeByte(restriction);
			});
			
			for (var payload : payloads.getBuffers()) {
				logger.debug(ByteBufUtil.hexDump(payload));
			}
			
			Util.release(payloads);
		} catch (Exception e) {
			logger.error("getLobbyList: Exception occurred.", e);
			Util.release(payloads);
		}
	}

}
