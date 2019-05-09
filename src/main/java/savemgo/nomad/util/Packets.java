package savemgo.nomad.util;

import io.netty.buffer.ByteBuf;
import savemgo.nomad.Constants;

public class Packets {

	// stuff for checking if crypto packet
	
	public static void xorScrambler(ByteBuf buffer, int index, int length) {
		int longCount = length >>> 3;
		int byteCount = length & 7;

		for (int i = longCount; i > 0; i--) {
			long l = buffer.getLong(index) ^ Constants.PACKET_SCRAMBLER_LONG;
			buffer.setLong(index, l);
			index += 8;
		}

		for (int i = byteCount; i > 0; i--) {
			byte b = (byte) (buffer.getByte(index) ^ Constants.PACKET_SCRAMBLER_BYTES[index % 4]);
			buffer.setByte(index, b);
			index++;
		}
	}
	
}
