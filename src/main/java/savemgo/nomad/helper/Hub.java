package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.Nomad;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.util.Buffers;

public class Hub {

	private static final Logger logger = LogManager.getLogger();

	/**
	 * TODO: Add support for constant packets (don't release after writing) ... we
	 * will need to release eventually though... might have to skip out on this if
	 * it has a bytebuf ... may be okay if we use a byte array?
	 * 
	 * TODO: Add support for having a result as the payload (actually write the
	 * result as the payload!)
	 */

	private static final Packet LOBBYLIST_START = new Packet(0x4901, 0xdeadbeef);
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

				bo.writeInt(i).writeInt(lobby.getType());
				Buffers.writeStringFill(bo, lobby.getName(), 16);
				Buffers.writeStringFill(bo, lobby.getIp(), 15);
				bo.writeShort(lobby.getPort()).writeShort(lobby.getPlayers()).writeShort(lobby.getId())
						.writeByte(restriction);
			});

			ctx.write(new Packet(0x2002));
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(0x2003, payload));
			}
			ctx.write(new Packet(0x2004));
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
