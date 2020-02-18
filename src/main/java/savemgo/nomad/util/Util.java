package savemgo.nomad.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import savemgo.nomad.database.record.Character;

public class Util {

	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static String getFullCharacterName(Character chara) {
		if (chara.getNamePrefix() != null) {
			return chara.getNamePrefix() + chara.getName();
		}
		return chara.getName();
	}

}
