package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.Nomad;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.ByteBufEx;

public class Hub {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet LOBBYLIST_START = new Packet(0x4901, 0);
	private static final Packet LOBBYLIST_END = new Packet(0x4903, 0);

	public static void getLobbyList(ChannelHandlerContext ctx) {
		PayloadGroup payloads = null;
		try {
			var lobbies = Nomad.get().getLobbies();
			var iterator = lobbies.iterator();

			payloads = Buffers.createPayloads(lobbies.size(), 0x2e, 22, (bo, i) -> {
				NomadLobby lobby = iterator.next();

				boolean beginner = false, expansion = false, noHeadshot = false;

				int restriction = 0;
				restriction |= beginner ? 0b1 : 0;
				restriction |= expansion ? 0b1000 : 0;
				restriction |= noHeadshot ? 0b10000 : 0;

				var box = ByteBufEx.get(bo);
				try {
					bo.writeInt(i).writeInt(lobby.getType());
					box.writeStringFill(lobby.getName(), 16).writeStringFill(lobby.getIp(), 15);
					bo.writeShort(lobby.getPort()).writeShort(lobby.getPlayers()).writeShort(lobby.getId())
							.writeByte(restriction);
				} finally {
					box.recycle();
				}
			});

//			ctx.write(LOBBYLIST_START);
//			ctx.write(new Packet());
//			ctx.write(LOBBYLIST_END);
		} catch (Exception e) {
			logger.error("getLobbyList: Exception occurred.", e);
			Buffers.release(payloads);
		}
	}

	public static void getNews(ChannelHandlerContext ctx) {

	}

}
