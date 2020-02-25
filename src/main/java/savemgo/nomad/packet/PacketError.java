package savemgo.nomad.packet;

public enum PacketError {

	/** General */
	CANTCONNECT(-0xf0, true), // start game error kicks out to title screen
	CANTUSE(-0xf1, true), // -0x11C, -0x123, can't use character
	GAMEIDINUSE(-0xf2, true), //
	NOT_IMPLEMENTED(0xff), //
	GENERAL(0x1), //
	INVALID_SESSION(0x2), //

	/** User */
	CHAR_NAMETAKEN(-0x104, true), // unable to select char
	CHAR_NAMEINVALID(-0x106, true), //
	CHAR_CANTDELETEYET(-0x10c, true), //
	CHAR_CREATEBANNED(-0x125, true), //
	CHAR_CANTDELETECLANLEADER(-0x4bc, true), //
	CHAR_CANTDELETECLANJOIN(-0x4bc, true), //

	/** Character */
	CHARACTER_DOESNOTEXIST(0x20), //

	/** Game */
	GAME_PLACEHOLDER(0x30), //

	/** Clan */
	CLAN_DOESNOTEXIST(0x40), //
	CLAN_NOTAMEMBER(0x41), //
	CLAN_INACLAN(0x42), //
	CLAN_NOAPPLICATION(0x43), //
	CLAN_HASAPPLICATION(0x44), //
	CLAN_NOTALEADER(0x45); //

	private int code;
	private boolean official = false;

	PacketError(int code) {
		this.code = code;
	}

	PacketError(int code, boolean official) {
		this.code = code;
		this.official = official;
	}

	public int getCode() {
		return code;
	}

	public boolean isOfficial() {
		return official;
	}

}