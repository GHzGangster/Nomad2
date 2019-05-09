package savemgo.nomad.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import savemgo.nomad.Constants;

public class Packets {

	private static final Logger logger = LogManager.getLogger();
	
	// stuff for checking if crypto packet
	
	public static void xorScrambler(ByteBuf buffer, int index, int length) {
		int longCount = length >>> 3;
		int byteCount = length & 7;
		
		logger.debug("{} {}", longCount, byteCount);

		for (int i = longCount; i > 0; i--) {
			logger.debug("long {}", index);
			long l = buffer.getLong(index) ^ Constants.PACKET_SCRAMBLER_LONG;
			buffer.setLong(index, l);
			index += 8;
		}

		for (int i = byteCount; i > 0; i--) {
			logger.debug("byte {}", index);
			byte b = (byte) (buffer.getByte(index) ^ Constants.PACKET_SCRAMBLER_BYTES[index % 4]);
			buffer.setByte(index, b);
			index++;
		}
	}
	
}
