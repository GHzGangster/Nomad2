package savemgo.nomad.helper;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.record.CharaBlocked;
import savemgo.nomad.database.record.CharaFriend;
import savemgo.nomad.local.util.LocalUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.GameError;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Characters {

	private static final Logger logger = LogManager.getLogger();

	private static final byte CHARACTERINFO_BYTES1[] = { (byte) 0x16, (byte) 0xAE,
			//
			(byte) 0x03, (byte) 0x38,
			//
			(byte) 0x01, (byte) 0x3E,
			//
			(byte) 0x01, (byte) 0x50 };

	private static final byte CHARACTERINFO_BYTES2[] = { (byte) 0x00,
			//
			(byte) 0xB7, (byte) 0xFD, (byte) 0xAB, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF, (byte) 0x7B, (byte) 0x00,
			(byte) 0x00, (byte) 0x00,
			//
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			//
			(byte) 0x00,
			//
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };

	public static void getCharacterInfo(ChannelHandlerContext ctx) {
		GameError error = null;
		ByteBuf bo = null;
		try (var handle = DB.open()) {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getCharacterInfo- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get friends and block lists
			var friends = handle.createQuery("SELECT target " //
					+ "FROM mgo2_charas_friends " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaFriend.class).list();

			var block = handle.createQuery("SELECT target " //
					+ "FROM mgo2_charas_blocked " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaBlocked.class).list();

			long time = Instant.now().getEpochSecond();
			int secondLastLogin = (int) time - 1;
			int lastLogin = (int) time;

			// Create payload
			bo = ctx.alloc().directBuffer(0x243);

			bo.writeInt(localChara.getId());
			Buffers.writeStringFill(bo, Util.getFullCharacterName(localChara), 16);
			bo.writeBytes(CHARACTERINFO_BYTES1);

			// TODO: Get exp
			int exp = 0;

			bo.writeInt(exp).writeInt(secondLastLogin).writeInt(lastLogin).writeZero(1);

			for (var friend : friends) {
				bo.writeInt(friend.getTarget());
			}
			bo.writeZero(0x129 - bo.writerIndex());

			for (var blocked : block) {
				bo.writeInt(blocked.getTarget());
			}
			bo.writeZero(0x229 - bo.writerIndex());

			bo.writeBytes(CHARACTERINFO_BYTES2);

			// Write payload
			ctx.write(new Packet(0x4101, bo));
		} catch (Exception e) {
			logger.error("getCharacterInfo- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4101, error);
		}
	}

}
