package savemgo.nomad.database.record;

public class Chara {

	private int id;
	private int user;
	private String name;
	private String oldName;
	private String namePrefix;
	private int rank;
	private String comment;
	private String gameplayOptions;
	private boolean active;
	private int creationTime;
	private int lobby;
	private int version;

	public Chara() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUser() {
		return user;
	}

	public void setUser(int user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getGameplayOptions() {
		return gameplayOptions;
	}

	public void setGameplayOptions(String gameplayOptions) {
		this.gameplayOptions = gameplayOptions;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(int creationTime) {
		this.creationTime = creationTime;
	}

	public int getLobby() {
		return lobby;
	}

	public void setLobby(int lobby) {
		this.lobby = lobby;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

}
