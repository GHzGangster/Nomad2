package savemgo.nomad.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.GameError;

public class Packets {

	public static byte[] KEY_KIT = null;

	public static void scramble(ByteBuf buffer, int index, int length) {
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

	public static int getResult(GameError error) {
		if (error.isOfficial()) {
			return error.getCode();
		}
		return Constants.PACKET_ERROR_MASK | error.getCode();
	}

	public static void writeError(ChannelHandlerContext ctx, int command, GameError error) {
		if (error != null) {
			int result = getResult(error);
			ctx.write(new Packet(command, result));
		}
	}

	static {
		KEY_KIT = Ptsys.decryptKey(Ptsys.KEY_6);
	}

}
