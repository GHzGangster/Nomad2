package savemgo.nomad.local;

public class LocalChara {

	// TODO: Make sure that we only really store things that we need to use more
	// than once, and doesn't change often.

	private int id;
	private LocalUser user;
	private String name;
	private String namePrefix;
	private boolean active;
	private LocalLobby lobby;

	public LocalChara() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public LocalUser getUser() {
		return user;
	}

	public void setUser(LocalUser user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalLobby getLobby() {
		return lobby;
	}

	public void setLobby(LocalLobby lobby) {
		this.lobby = lobby;
	}

}
