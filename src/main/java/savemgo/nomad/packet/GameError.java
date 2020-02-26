package savemgo.nomad.packet;

public enum GameError {

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
	LOBBY_IS_FULL(-0x191, true), //
	CHAR_CANTBEUSED(-0x192, true), //
	LOBBY_NO_ACCESS(-0x193, true), // -0x194, -0x195
	LOBBY_IS_BUSY(-0x196, true), //
	CHARACTER_LOGIN_RESTRICTED(-0x197, true), //
	LOBBY_TOURNAMENT_TOO_EARLY(-0x44d, true), //
	LOBBY_EXCEEDED_MATCHES(-0x460, true), // -0x461
	LOBBY_TOURNAMENT_TOO_LATE(-0x462, true), //
	LOBBY_TOURNAMENT_PLAYERS_ONLY(-0x463, true), //
	CHAR_CANTDELETECLANLEADER(-0x4bc, true), //
	CHAR_CANTDELETECLANJOIN(-0x4bc, true), //

	/** Mail */
	MAIL_BAD_ADDRESS(-0x321, true), //
	MAIL_RECEIVER_INBOX_FULL(-0x322, true), //
	MAIL_RECEIVER_BLOCKLIST(-0x32a, true), //
	MAIL_BAD_ADDRESS2(-0x334, true), //
	MAIL_RECEIVER_BLOCKMAIL(-0x33e, true), //
	MAIL_SERVICES_BANNED(-0x33f, true), //
	MAIL_SUBJECT_BADWORD(-0x340, true), //
	MAIL_MESSAGE_BADWORD(-0x341, true), //
	CLAN_SERVICES_BANNED(-0x4ce, true), //
	
	/** Character */
	CHARACTER_DOESNOTEXIST(0x20), //

	/** Game */
	GAME_PLACEHOLDER(0x30), //
	GAME_FULL(-0x1f7, true), //
	GAME_WRONG_PASSWORD(-0x21c, true), //
	GAME_JOIN_BANNED(-0x21d, true), //
	GAME_TRAINING_CLOSED(-0x21e, true), //

	/** Clan */
	CLAN_DOESNOTEXIST(0x40), //
	CLAN_NOTAMEMBER(0x41), //
	CLAN_INACLAN(0x42), //
	CLAN_NOAPPLICATION(0x43), //
	CLAN_HASAPPLICATION(0x44), //
	CLAN_NOTALEADER(0x45); //

	private int code;
	private boolean official = false;

	GameError(int code) {
		this.code = code;
	}

	GameError(int code, boolean official) {
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