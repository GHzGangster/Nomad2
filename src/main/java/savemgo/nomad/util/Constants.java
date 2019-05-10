package savemgo.nomad.util;

import javax.crypto.spec.SecretKeySpec;

public class Constants {

	///////////
	// Server
	///////////

	public static final int SERVER_CLIENT_MAX = 2010;
	public static final int SERVER_CLIENT_RCVBUF = 0x4000;

	///////////
	// Packet
	///////////

	public static final int PACKET_PAYLOAD_MAX = 0x3ff;

	public static final int PACKET_SCRAMBLER = 0x5a7085af;
	public static final long PACKET_SCRAMBLER_LONG = ((PACKET_SCRAMBLER & 0xffffffffL) << 32) | PACKET_SCRAMBLER;
	public static final int PACKET_SCRAMBLER_HIGH = (PACKET_SCRAMBLER >> 16) & 0xffff;
	public static final int PACKET_SCRAMBLER_LOW = PACKET_SCRAMBLER & 0xffff;
	public static final byte[] PACKET_SCRAMBLER_BYTES = Util.intToBytes(PACKET_SCRAMBLER);

	public static final byte[] PACKET_HMAC = new byte[] { (byte) 0x5A, (byte) 0x37, (byte) 0x2F, (byte) 0x62,
			(byte) 0x69, (byte) 0x4A, (byte) 0x34, (byte) 0x36, (byte) 0x54, (byte) 0x7A, (byte) 0x47, (byte) 0x46,
			(byte) 0x2D, (byte) 0x38, (byte) 0x79, (byte) 0x78 };

	public static final SecretKeySpec PACKET_HMAC_SPEC = new SecretKeySpec(PACKET_HMAC, "HmacMD5");

	public static final int[] PACKET_CRYPTED_IN = { 0x3003, 0x4310, 0x4320, 0x43c0, 0x4700, 0x4990 };
	public static final int[] PACKET_CRYPTED_OUT = { 0x4305 };

	public static final byte[] PACKET_MIO = { (byte) 0x27, (byte) 0x50, (byte) 0x1F, (byte) 0xD0, (byte) 0x4E,
			(byte) 0x6B, (byte) 0x82, (byte) 0xC8, (byte) 0x31, (byte) 0x02, (byte) 0x4D, (byte) 0xAC, (byte) 0x5C,
			(byte) 0x63, (byte) 0x05, (byte) 0x22, (byte) 0x19, (byte) 0x74, (byte) 0xDE, (byte) 0xB9, (byte) 0x38,
			(byte) 0x8A, (byte) 0x21, (byte) 0x90, (byte) 0x1D, (byte) 0x57, (byte) 0x6C, (byte) 0xBB, (byte) 0xE2,
			(byte) 0xF3, (byte) 0x77, (byte) 0xEF, (byte) 0x23, (byte) 0xD7, (byte) 0x54, (byte) 0x86, (byte) 0x01,
			(byte) 0x0F, (byte) 0x37, (byte) 0x81, (byte) 0x9A, (byte) 0xFE, (byte) 0x6C, (byte) 0x32, (byte) 0x1A,
			(byte) 0x01, (byte) 0x46, (byte) 0xD2, (byte) 0x15, (byte) 0x44, (byte) 0xEC, (byte) 0x36, (byte) 0x5B,
			(byte) 0xF7, (byte) 0x28, (byte) 0x9A };

	public static final int PACKET_ERROR_MASK = 0xC0FFEE << 8;

}
