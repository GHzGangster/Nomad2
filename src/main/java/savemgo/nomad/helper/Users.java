package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.database.DB;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.ResultError;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;

public class Users {

	private static final Logger logger = LogManager.getLogger();

	public static void setSession(ChannelHandlerContext ctx, Packet in) {
		ResultError error = null;
		try (var handle = DB.open()) {
			var bi = in.getPayload();

			// Get id
			int id = bi.readInt();

			// Get session id
			// TODO: Use 16 char session ids in gidauth, and don't trim here
			Ptsys.encryptBlowfish(Packets.KEY_KIT, bi, 0x4, bi, 0x4, 0x10);
			var sessionId = Buffers.readString(bi, 0x10);
			sessionId = sessionId.substring(0, 8);
			logger.debug("Session id: {}", sessionId);

			// Check session id
			var session = handle.createQuery("SELECT * FROM users WHERE mgo2_session=:sessionId")
					.bind("sessionId", sessionId).mapToMap().findOne();
			if (session.isEmpty()) {
				error = ResultError.INVALID_SESSION;
				logger.error("Invalid session: {}", sessionId);
				return;
			}
			
			ctx.write(new Packet(0x3004, 0));
		} catch (Exception e) {
			logger.error("setSession: Exception occurred.", e);
			error = ResultError.GENERAL;
		} finally {
			Packets.writeError(ctx, 0x3004, error);
		}
	}

}
