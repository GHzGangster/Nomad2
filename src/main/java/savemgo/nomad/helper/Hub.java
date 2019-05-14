package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.Nomad;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.server.NomadLobby;
import savemgo.nomad.util.Buffers;

public class Hub {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet LOBBYLIST_START = new Packet(0x2002,
			new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });
	private static final Packet LOBBYLIST_END = new Packet(0x2004, 0);

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

				bo.writeInt(i).writeInt(lobby.getType());
				Buffers.writeStringFill(bo, lobby.getName(), 16);
				Buffers.writeStringFill(bo, lobby.getIp(), 15);
				bo.writeShort(lobby.getPort()).writeShort(lobby.getPlayers()).writeShort(lobby.getId())
						.writeByte(restriction);
			});

			ctx.write(LOBBYLIST_START);
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(0x2003, payload));
			}
			ctx.write(LOBBYLIST_END);
		} catch (Exception e) {
			logger.error("getLobbyList: Exception occurred.", e);
			Buffers.release(payloads);
		}
	}

	public static void test(ChannelHandlerContext ctx) {

	}

	public static void getNews(ChannelHandlerContext ctx) {

	}

}
