package savemgo.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.ByteBuf;
import savemgo.nomad.packet.Packet;

public class Util {

	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static void release(ByteBuf buffer) {
		if (buffer != null) {
			buffer.release();
		}
	}
	
	public static void release(Packet packet) {
		if (packet != null) {
			release(packet.getPayload());
		}
	}
	
}
