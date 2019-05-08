package savemgo.nomad;

import java.util.List;

public class Config {

	private String databaseUrl;
	private String databaseUsername;
	private String databasePassword;
	private int databaseWorkers;
	private int databasePoolMin;
	private int databasePoolMax;
	private int databasePoolIncrement;
	private int serverWorkers;
	private List<Integer> lobbies;
	private String plugin;

	public Config() {

	}

	public String getDatabaseUrl() {
		return databaseUrl;
	}

	public void setDatabaseUrl(String databaseUrl) {
		this.databaseUrl = databaseUrl;
	}

	public String getDatabaseUsername() {
		return databaseUsername;
	}

	public void setDatabaseUsername(String databaseUsername) {
		this.databaseUsername = databaseUsername;
	}

	public String getDatabasePassword() {
		return databasePassword;
	}

	public void setDatabasePassword(String databasePassword) {
		this.databasePassword = databasePassword;
	}

	public int getDatabaseWorkers() {
		return databaseWorkers;
	}

	public void setDatabaseWorkers(int databaseWorkers) {
		this.databaseWorkers = databaseWorkers;
	}

	public int getDatabasePoolMin() {
		return databasePoolMin;
	}

	public void setDatabasePoolMin(int databasePoolMin) {
		this.databasePoolMin = databasePoolMin;
	}

	public int getDatabasePoolMax() {
		return databasePoolMax;
	}

	public void setDatabasePoolMax(int databasePoolMax) {
		this.databasePoolMax = databasePoolMax;
	}

	public int getServerWorkers() {
		return serverWorkers;
	}

	public void setServerWorkers(int serverWorkers) {
		this.serverWorkers = serverWorkers;
	}

	public List<Integer> getLobbies() {
		return lobbies;
	}

	public void setLobbies(List<Integer> lobbies) {
		this.lobbies = lobbies;
	}

	public String getPlugin() {
		return plugin;
	}

	public void setPlugin(String plugin) {
		this.plugin = plugin;
	}

	public int getDatabasePoolIncrement() {
		return databasePoolIncrement;
	}

	public void setDatabasePoolIncrement(int databasePoolIncrement) {
		this.databasePoolIncrement = databasePoolIncrement;
	}

}
