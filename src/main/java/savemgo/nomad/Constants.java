package savemgo.nomad;

import savemgo.nomad.util.Util;

public class Constants {
	
	///////////
	// Server 
	///////////
	
	public static final int SERVER_CLIENT_MAX = 2010;
	public static final int SERVER_CLIENT_RCVBUF = 0x4000;
	
	
	///////////
	// Packet
	///////////
	
	public static final int PACKET_HEADER = 0x18;
	public static final int PACKET_PAYLOAD_MAX = 0x3ff;
	
	public static final int PACKET_SCRAMBLER = 0;//0x5a7085af;
	public static final int PACKET_SCRAMBLER_LONG = PACKET_SCRAMBLER | (PACKET_SCRAMBLER << 32);
	public static final int PACKET_SCRAMBLER_HIGH = (PACKET_SCRAMBLER >> 16) & 0xffff;
	public static final int PACKET_SCRAMBLER_LOW = PACKET_SCRAMBLER & 0xffff;
	public static final byte[] PACKET_SCRAMBLER_BYTES = Util.intToBytes(PACKET_SCRAMBLER);
	
	public static final byte[] PACKET_HMAC = new byte[] { (byte) 0x5A, (byte) 0x37, (byte) 0x2F, (byte) 0x62, (byte) 0x69,
			(byte) 0x4A, (byte) 0x34, (byte) 0x36, (byte) 0x54, (byte) 0x7A, (byte) 0x47, (byte) 0x46, (byte) 0x2D,
			(byte) 0x38, (byte) 0x79, (byte) 0x78 };

}
