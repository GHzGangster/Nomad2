package savemgo.nomad.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import savemgo.nomad.database.record.Chara;
import savemgo.nomad.local.LocalCharacter;

public class Util {

	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static String getFullCharacterName(Chara chara) {
		if (chara.getNamePrefix() != null) {
			return chara.getNamePrefix() + chara.getName();
		}
		return chara.getName();
	}

	public static String getFullCharacterName(LocalCharacter chara) {
		if (chara.getNamePrefix() != null) {
			return chara.getNamePrefix() + chara.getName();
		}
		return chara.getName();
	}

}
