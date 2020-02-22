package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.Nomad;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.record.News;
import savemgo.nomad.local.util.LocalLobbies;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PacketError;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.server.LobbyHandler;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;

public class Hub {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet LOBBYLIST_START = new Packet(0x2002,
			new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });
	private static final Packet LOBBYLIST_END = new Packet(0x2004, 0);

	public static void getLobbyList(ChannelHandlerContext ctx) {
		PacketError error = null;
		PayloadGroup payloads = null;
		try {
			// Get lobbies running on this instance
			var lobbies = LocalLobbies.get();
			var iterator = lobbies.iterator();

			// Create payloads
			payloads = Buffers.createPayloads(lobbies.size(), 0x2e, 22, (bo, i) -> {
				var lobby = iterator.next();

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

			// Write payloads
			ctx.write(LOBBYLIST_START);
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(0x2003, payload));
			}
			ctx.write(LOBBYLIST_END);
		} catch (Exception e) {
			logger.error("getLobbyList- Exception occurred.", e);
			Buffers.release(payloads);
			error = PacketError.GENERAL;
		} finally {
			Packets.writeError(ctx, 0x2002, error);
		}
	}

	private static final Packet NEWSLIST_START = new Packet(0x2009,
			new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });
	private static final Packet NEWSLIST_END = new Packet(0x200b, 0);

	public static void getNewsList(ChannelHandlerContext ctx) {
		PacketError error = null;
		PayloadGroup payloads = null;
		try (var handle = DB.open()) {
			// Get news
			var news = handle.createQuery("""
					SELECT id, time, important, topic, message
					FROM mgo2_news ORDER BY id DESC
					""").mapToBean(News.class).list();

			// Create payloads
			payloads = new PayloadGroup();
			payloads.setBuffers(new ByteBuf[news.size()]);

			for (int i = 0; i < news.size(); i++) {
				var item = news.get(i);

				// Calculate message length so it doesn't overflow
				int messageLength = Math.min(item.getMessage().length(), 885);

				// Create buffer
				payloads.getBuffers()[i] = Buffers.ALLOCATOR.directBuffer(138 + messageLength);
				var bo = payloads.getBuffers()[i];

				// Write news item
				bo.writeInt(item.getId()).writeBoolean(item.isImportant()).writeInt(item.getTime());
				Buffers.writeStringFill(bo, item.getTopic(), 128);
				Buffers.writeStringFill(bo, item.getMessage(), messageLength + 1);
			}

			// Write payloads
			ctx.write(NEWSLIST_START);
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(0x200a, payload));
			}
			ctx.write(NEWSLIST_END);
		} catch (Exception e) {
			logger.error("getNewsList- Exception occurred.", e);
			Buffers.release(payloads);
			error = PacketError.GENERAL;
		} finally {
			Packets.writeError(ctx, 0x2009, error);
		}
	}

}
