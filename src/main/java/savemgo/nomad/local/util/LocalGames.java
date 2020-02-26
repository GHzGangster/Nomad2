package savemgo.nomad.local.util;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import savemgo.nomad.local.LocalGame;

public class LocalGames {

	private static final ConcurrentHashMap<Integer, LocalGame> GAMES = new ConcurrentHashMap<>();

	public static void add(LocalGame game) {
		GAMES.put(game.getId(), game);
	}

	public static void remove(LocalGame game) {
		GAMES.remove(game.getId());
	}

	public static Collection<LocalGame> get() {
		return GAMES.values();
	}

	public static LocalGame get(int id) {
		return GAMES.get(id);
	}

	public static ConcurrentHashMap<Integer, LocalGame> getGames() {
		return GAMES;
	}

}
