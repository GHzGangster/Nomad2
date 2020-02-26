package savemgo.nomad.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import savemgo.nomad.database.record.Chara;
import savemgo.nomad.local.LocalChara;

public class Util {

	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static final Gson GSON = new GsonBuilder().serializeNulls().create();

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static String getFullCharacterName(Chara chara) {
		if (chara.getNamePrefix() != null) {
			return chara.getNamePrefix() + chara.getName();
		}
		return chara.getName();
	}

	public static String getFullCharacterName(LocalChara chara) {
		if (chara.getNamePrefix() != null) {
			return chara.getNamePrefix() + chara.getName();
		}
		return chara.getName();
	}

	private static final char[] NAME_VALID_CHARS = { ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',',
			'-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?', '@', 'A',
			'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			'W', 'X', 'Y', 'Z', '[', ']', '^', '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', '¡', '¢', '£', '¥',
			'¦', 'ª', '«', '°', 'µ', 'º', '»', '¿', 'À', 'Á', 'Â', 'Ã', 'Ä', 'Å', 'Æ', 'Ç', 'È', 'É', 'Ê', 'Ë', 'Ì',
			'Í', 'Î', 'Ï', 'Ñ', 'Ò', 'Ó', 'Ô', 'Õ', 'Ö', '×', 'Ø', 'Ù', 'Ú', 'Û', 'Ü', 'Ý', 'ß', 'à', 'á', 'â', 'ã',
			'ä', 'å', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', '÷', 'ø', 'ù',
			'ú', 'û', 'ü', 'ý', 'ÿ' };

	public static boolean checkName(String str) {
		char[] chars = str.toCharArray();
		if (chars[0] == ' ' || chars[chars.length - 1] == ' ') {
			return false;
		}
		charLoop: for (char c : chars) {
			for (char v : NAME_VALID_CHARS) {
				if (c == v) {
					continue charLoop;
				}
			}
			return false;
		}
		return true;
	}

}
