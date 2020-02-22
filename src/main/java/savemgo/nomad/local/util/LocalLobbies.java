package savemgo.nomad.local.util;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import savemgo.nomad.local.LocalLobby;

public class LocalLobbies {

	private static final ConcurrentHashMap<Integer, LocalLobby> LOBBIES = new ConcurrentHashMap<>();

	public static void add(LocalLobby lobby) {
		LOBBIES.put(lobby.getId(), lobby);
	}

	public static void remove(LocalLobby lobby) {
		LOBBIES.remove(lobby.getId());
	}

	public static Collection<LocalLobby> get() {
		return LOBBIES.values();
	}

	public static LocalLobby get(int id) {
		return LOBBIES.get(id);
	}

}
