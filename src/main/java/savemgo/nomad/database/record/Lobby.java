package savemgo.nomad.database.record;

public class Lobby {

	private int id;
	private int type;
	private int subtype;
	private String name;
	private String ip;
	private int port;
	private int players;
	private String settings;
	private int version;

	public Lobby() {

	}

	public Lobby(int id, int type, int subtype, String name, String ip, int port, int players, String settings,
			int version) {
		this.id = id;
		this.type = type;
		this.subtype = subtype;
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.players = players;
		this.settings = settings;
		this.version = version;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getSubtype() {
		return subtype;
	}

	public void setSubtype(int subtype) {
		this.subtype = subtype;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPlayers() {
		return players;
	}

	public void setPlayers(int players) {
		this.players = players;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String settings) {
		this.settings = settings;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}
