package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.ByteBufEx;
import savemgo.nomad.util.Packets;

public class Users {

	private static final Logger logger = LogManager.getLogger();

	public static void setSession(ChannelHandlerContext ctx, Packet in) {
		ByteBufEx bix = null;
		try {
			var bi = in.getPayload();

			// Get id
			int id = bi.readInt();

			// Get session id
			Ptsys.encryptBlowfish(Packets.KEY_KIT, bi, 0x4, bi, 0x4, 0x10);
			bix = ByteBufEx.get(bi);
			var sessionId = bix.readString(0x10);

			logger.debug("Session id: {}", sessionId);
		} catch (Exception e) {
			logger.error("setSession: Exception occurred.", e);
		} finally {
			if (bix != null) {
				bix.recycle();
			}
		}
	}

}
