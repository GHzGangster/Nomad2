package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.Util;
import savemgo.nomad.packet.PayloadGroup;

public class Hub {

	private static final Logger logger = LogManager.getLogger();

	public static void getLobbyList(ChannelHandlerContext ctx) {
		PayloadGroup payloads = null;
		try {
			payloads = Util.createPayloads(7, 0x8, 4, (bb, i) -> {
				bb.writeInt(0xcafebabe);
			});
		} catch (Exception e) {
			logger.error("getLobbyList: Exception occurred.", e);
			Util.release(payloads);
		}
	}

}
