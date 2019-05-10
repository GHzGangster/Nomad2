package savemgo.nomad.crypto.ptsys;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import net.sourceforge.blowfishj.crypt.BlowfishECB;

public class Ptsys {

	private static final Logger logger = LogManager.getLogger();

	/**
	 * Base key
	 */
	public static final byte KEY_0[] = { (byte) 0x74, (byte) 0xF6, (byte) 0x6D, (byte) 0xC2, (byte) 0x85, (byte) 0x98,
			(byte) 0xF5, (byte) 0xD1, (byte) 0x72, (byte) 0xAC, (byte) 0x2D, (byte) 0xCA, (byte) 0xCE, (byte) 0x55,
			(byte) 0x44, (byte) 0xD6, (byte) 0x65, (byte) 0xF1, (byte) 0x1D, (byte) 0x05, (byte) 0xBE, (byte) 0xA2,
			(byte) 0x05, (byte) 0x68, (byte) 0xE7, (byte) 0x6C, (byte) 0x52, (byte) 0x9D, (byte) 0xEB, (byte) 0x35,
			(byte) 0x89, (byte) 0x0E, (byte) 0xC3, (byte) 0x32, (byte) 0xFF, (byte) 0x24, (byte) 0xFE, (byte) 0x5D,
			(byte) 0x9C, (byte) 0x3F, (byte) 0xB3, (byte) 0x41, (byte) 0x89, (byte) 0xCF, (byte) 0x47, (byte) 0x05,
			(byte) 0x5B, (byte) 0x26, (byte) 0xF9, (byte) 0xE4, (byte) 0xCC, (byte) 0x63, (byte) 0x9A, (byte) 0x46,
			(byte) 0xB5, (byte) 0x46, (byte) 0x54, (byte) 0x04, (byte) 0xDF, (byte) 0x41, (byte) 0xE6, (byte) 0x5B,
			(byte) 0x8E, (byte) 0x4E };

	/**
	 * Used for file encryption
	 */
	public static final byte KEY_3[] = { (byte) 0x53, (byte) 0x08, (byte) 0x57, (byte) 0x88, (byte) 0x72, (byte) 0x0C,
			(byte) 0xC9, (byte) 0x55, (byte) 0xD1, (byte) 0xA7, (byte) 0x5F, (byte) 0xCA, (byte) 0x0A, (byte) 0x98,
			(byte) 0x8C, (byte) 0xED, (byte) 0x84, (byte) 0xCF, (byte) 0xBA, (byte) 0x8B, (byte) 0xFD, (byte) 0xDA,
			(byte) 0x9A, (byte) 0x04, (byte) 0x6A, (byte) 0xF0, (byte) 0xFB, (byte) 0x4D, (byte) 0xE0, (byte) 0x27,
			(byte) 0xDC, (byte) 0x24, (byte) 0xB2, (byte) 0xB6, (byte) 0x36, (byte) 0x11, (byte) 0x0D, (byte) 0x27,
			(byte) 0xCA, (byte) 0x28, (byte) 0x4E, (byte) 0x0A, (byte) 0xB1, (byte) 0x59, (byte) 0x12, (byte) 0x21,
			(byte) 0x25, (byte) 0x93, (byte) 0xB5, (byte) 0x2D, (byte) 0x94, (byte) 0x5C, (byte) 0x63, (byte) 0x3A,
			(byte) 0x0B, (byte) 0x53, (byte) 0x97, (byte) 0xD4, (byte) 0x1B, (byte) 0x64, (byte) 0xF7, (byte) 0x0E,
			(byte) 0xD1, (byte) 0xEE };

	/**
	 * Kit 1.34. Used for MGO2 server communication.
	 */
	public static final byte KEY_6[] = { (byte) 0x06, (byte) 0xCC, (byte) 0x2D, (byte) 0x03, (byte) 0x12, (byte) 0x79,
			(byte) 0x09, (byte) 0x56, (byte) 0x7B, (byte) 0x1B, (byte) 0x25, (byte) 0x86, (byte) 0xE2, (byte) 0x8F,
			(byte) 0x3A, (byte) 0x5A, (byte) 0xEF, (byte) 0xD5, (byte) 0x81, (byte) 0x72, (byte) 0x9A, (byte) 0x14,
			(byte) 0x09, (byte) 0xD6, (byte) 0xA1, (byte) 0x7F, (byte) 0x62, (byte) 0xB6, (byte) 0x52, (byte) 0x97,
			(byte) 0x90, (byte) 0xC5, (byte) 0x35, (byte) 0x0F, (byte) 0x1C, (byte) 0xAB, (byte) 0x37, (byte) 0x91,
			(byte) 0x40, (byte) 0x73, (byte) 0x6B, (byte) 0xCC, (byte) 0x9B, (byte) 0x2C, (byte) 0xA1, (byte) 0x78,
			(byte) 0x16, (byte) 0x96, (byte) 0x8D, (byte) 0xFA, (byte) 0x4C, (byte) 0x71, (byte) 0x17, (byte) 0xB5,
			(byte) 0xB3, (byte) 0xB0, (byte) 0x5C, (byte) 0x0E, (byte) 0xD7, (byte) 0x26, (byte) 0xC9, (byte) 0xA3,
			(byte) 0x9D, (byte) 0xB6 };

	public static final byte KEY_11[] = { (byte) 0x0E, (byte) 0xE5, (byte) 0x76, (byte) 0xAD, (byte) 0xA1, (byte) 0x1C,
			(byte) 0xDA, (byte) 0xFF, (byte) 0x2B, (byte) 0x91, (byte) 0x48, (byte) 0x07, (byte) 0xAF, (byte) 0x9A,
			(byte) 0xF1, (byte) 0x4C, (byte) 0xB6, (byte) 0x95, (byte) 0x9E, (byte) 0x94, (byte) 0x6D, (byte) 0x25,
			(byte) 0x03, (byte) 0x26, (byte) 0x88, (byte) 0xBB, (byte) 0xDB, (byte) 0xE5, (byte) 0xE9, (byte) 0x59,
			(byte) 0x06, (byte) 0x02, (byte) 0x47, (byte) 0xED, (byte) 0x56, (byte) 0xD0, (byte) 0x3C, (byte) 0xC9,
			(byte) 0xA9, (byte) 0x21, (byte) 0xE6, (byte) 0xCD, (byte) 0x4E, (byte) 0x72, (byte) 0xF5, (byte) 0xB7,
			(byte) 0xD5, (byte) 0x76, (byte) 0x4D, (byte) 0x75, (byte) 0xB6, (byte) 0x4E, (byte) 0xF6, (byte) 0x2D,
			(byte) 0x0D, (byte) 0x83, (byte) 0x14, (byte) 0x58, (byte) 0xC2, (byte) 0x02, (byte) 0x2A, (byte) 0xDF,
			(byte) 0xC6, (byte) 0xFA };

	/**
	 * Same process as decryptBlowfish
	 */
	public static byte[] decryptKey(byte[] key) {
		byte[] result = new byte[key.length];

		byte[] keyIv = new byte[8];
		byte[] keyBlowfish = new byte[KEY_0.length - keyIv.length];
		System.arraycopy(KEY_0, 0, keyIv, 0, keyIv.length);
		System.arraycopy(KEY_0, keyIv.length, keyBlowfish, 0, keyBlowfish.length);

		BlowfishECB blowfish = new BlowfishECB(keyBlowfish, 0, keyBlowfish.length);

		byte[] last = new byte[8];
		System.arraycopy(keyIv, 0, last, 0, last.length);
		for (int i = 0; i < key.length / 8; i++) {
			blowfish.decrypt(key, i * 8, result, i * 8, 8);
			for (int j = 0; j < 8; j++) {
				result[i * 8 + j] ^= last[j];
			}
			System.arraycopy(key, i * 8, last, 0, 8);
		}

		return result;
	}

	/**
	 * Same process as encryptBlowfish
	 */
	public static byte[] encryptKey(byte[] key) {
		byte[] result = new byte[key.length];

		byte[] keyIv = new byte[8];
		byte[] keyBlowfish = new byte[KEY_0.length - keyIv.length];
		System.arraycopy(KEY_0, 0, keyIv, 0, keyIv.length);
		System.arraycopy(KEY_0, keyIv.length, keyBlowfish, 0, keyBlowfish.length);

		BlowfishECB blowfish = new BlowfishECB(keyBlowfish, 0, keyBlowfish.length);

		byte[] last = new byte[8];
		System.arraycopy(keyIv, 0, last, 0, last.length);
		for (int i = 0; i < key.length / 8; i++) {
			for (int j = 0; j < 8; j++) {
				result[i * 8 + j] = (byte) (key[i * 8 + j] ^ last[j]);
			}
			blowfish.encrypt(result, i * 8, result, i * 8, 8);
			System.arraycopy(result, i * 8, last, 0, 8);
		}

		return result;
	}

	public static void encryptBlowfish(byte[] key, ByteBuf bi, int inOffset, ByteBuf bo, int outOffset, int length) {
		byte[] keyIv = new byte[8];
		byte[] keyBlowfish = new byte[key.length - keyIv.length];
		System.arraycopy(key, 0, keyIv, 0, keyIv.length);
		System.arraycopy(key, keyIv.length, keyBlowfish, 0, keyBlowfish.length);

		BlowfishECB blowfish = new BlowfishECB(keyBlowfish, 0, keyBlowfish.length);

		byte[] in = new byte[8], out = new byte[8], last = new byte[8];
		System.arraycopy(keyIv, 0, last, 0, last.length);
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			for (int j = 0; j < 8; j++) {
				out[j] = (byte) (in[j] ^ last[j]);
			}
			blowfish.encrypt(out, 0, out, 0, 8);
			System.arraycopy(out, 0, last, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

	public static void decryptBlowfish(byte[] key, ByteBuf bi, int inOffset, ByteBuf bo, int outOffset, int length) {
		byte[] keyIv = new byte[8];
		byte[] keyBlowfish = new byte[key.length - keyIv.length];
		System.arraycopy(key, 0, keyIv, 0, keyIv.length);
		System.arraycopy(key, keyIv.length, keyBlowfish, 0, keyBlowfish.length);

		BlowfishECB blowfish = new BlowfishECB(keyBlowfish, 0, keyBlowfish.length);

		byte[] in = new byte[8], out = new byte[8], last = new byte[8];
		System.arraycopy(keyIv, 0, last, 0, last.length);
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			blowfish.decrypt(in, 0, out, 0, 8);
			for (int j = 0; j < 8; j++) {
				out[j] ^= last[j];
			}
			System.arraycopy(in, 0, last, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

	public static void decrypt(byte[] key, ByteBuf bi, int inOffset, ByteBuf bo, int outOffset, int length) {
		byte[] keyIv = new byte[8];
		byte[] keyXor = new byte[key.length - keyIv.length];
		System.arraycopy(key, 0, keyIv, 0, keyIv.length);
		System.arraycopy(key, keyIv.length, keyXor, 0, keyXor.length);

		byte[] in = new byte[8], out = new byte[8], last = new byte[8];
		System.arraycopy(keyIv, 0, last, 0, last.length);
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			for (int j = 0; j < 8; j++) {
				out[j] = (byte) (in[j] ^ last[j] ^ keyXor[i * 8 % keyXor.length + j]);
			}
			System.arraycopy(in, 0, last, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

	public static void encrypt(byte[] key, ByteBuf bi, int inOffset, ByteBuf bo, int outOffset, int length) {
		byte[] keyIv = new byte[8];
		byte[] keyXor = new byte[key.length - keyIv.length];
		System.arraycopy(key, 0, keyIv, 0, keyIv.length);
		System.arraycopy(key, keyIv.length, keyXor, 0, keyXor.length);

		byte[] in = new byte[8], out = new byte[8], last = new byte[8];
		System.arraycopy(keyIv, 0, last, 0, last.length);
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			for (int j = 0; j < 8; j++) {
				out[j] = (byte) (in[j] ^ last[j] ^ keyXor[i * 8 % keyXor.length + j]);
			}
			System.arraycopy(out, 0, last, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

	public static void encryptBlowfishSimple(byte[] keyBlowfish, ByteBuf bi, int inOffset, ByteBuf bo, int outOffset,
			int length) {
		BlowfishECB blowfish = new BlowfishECB(keyBlowfish, 0, keyBlowfish.length);

		byte[] in = new byte[8], out = new byte[8];
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			blowfish.encrypt(in, 0, out, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

	public static void decryptBlowfishSimple(byte[] keyBlowfish, ByteBuf bi, int inOffset, ByteBuf bo, int outOffset,
			int length) {
		BlowfishECB blowfish = new BlowfishECB(keyBlowfish, 0, keyBlowfish.length);

		byte[] in = new byte[8], out = new byte[8];
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			blowfish.decrypt(in, 0, out, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

	public static void encryptBlowfish(PtsysState state, byte[] key, ByteBuf bi, int inOffset, ByteBuf bo,
			int outOffset, int length) {
		if (!state.ready) {
			System.arraycopy(key, 0, state.last, 0, 0x8);

			byte[] keyBlowfish = new byte[0x38];
			System.arraycopy(key, 0x8, keyBlowfish, 0, 0x38);
			state.blowfish = new BlowfishECB(keyBlowfish, 0, 0x38);

			state.ready = true;
		}

		byte[] in = new byte[8], out = new byte[8];
		for (int i = 0; i < length / 8; i++) {
			bi.getBytes(inOffset + i * 8, in);
			for (int j = 0; j < 8; j++) {
				out[j] = (byte) (in[j] ^ state.last[j]);
			}
			state.blowfish.encrypt(out, 0, out, 0, 8);
			System.arraycopy(out, 0, state.last, 0, 8);
			bo.setBytes(outOffset + i * 8, out);
		}
	}

}
