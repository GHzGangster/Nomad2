package savemgo.nomad.local;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LocalGame {

	private int id;
	private LocalChara host;
	private LocalLobby lobby;
	private String name;
	private String password;
	private String comment;
	private int maxPlayers;
	private int currentGame;
	private String games;
	private int stance;
	private int ping;
	private String common;
	private String rules;

	private ReadWriteLock playersLock = new ReentrantReadWriteLock();
	private List<LocalPlayer> players = new ArrayList<>();

	public LocalGame() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public LocalChara getHost() {
		return host;
	}

	public void setHost(LocalChara host) {
		this.host = host;
	}

	public LocalLobby getLobby() {
		return lobby;
	}

	public void setLobby(LocalLobby lobby) {
		this.lobby = lobby;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public int getCurrentGame() {
		return currentGame;
	}

	public void setCurrentGame(int currentGame) {
		this.currentGame = currentGame;
	}

	public String getGames() {
		return games;
	}

	public void setGames(String games) {
		this.games = games;
	}

	public int getStance() {
		return stance;
	}

	public void setStance(int stance) {
		this.stance = stance;
	}

	public int getPing() {
		return ping;
	}

	public void setPing(int ping) {
		this.ping = ping;
	}

	public String getCommon() {
		return common;
	}

	public void setCommon(String common) {
		this.common = common;
	}

	public String getRules() {
		return rules;
	}

	public void setRules(String rules) {
		this.rules = rules;
	}

	public ReadWriteLock getPlayersLock() {
		return playersLock;
	}

	public List<LocalPlayer> getPlayers() {
		return players;
	}

}
