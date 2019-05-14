package savemgo.nomad.database.record;

public class User {

	private int id;
	private String username;
	private int role;
	private int bannedUntil;
	private int isCfw;
	private int slots;
	private int chara;
	private int version;

	public User() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getRole() {
		return role;
	}

	public void setRole(int role) {
		this.role = role;
	}

	public int getBannedUntil() {
		return bannedUntil;
	}

	public void setBannedUntil(int bannedUntil) {
		this.bannedUntil = bannedUntil;
	}

	public int getIsCfw() {
		return isCfw;
	}

	public void setIsCfw(int isCfw) {
		this.isCfw = isCfw;
	}

	public int getSlots() {
		return slots;
	}

	public void setSlots(int slots) {
		this.slots = slots;
	}

	public int getChara() {
		return chara;
	}

	public void setChara(int chara) {
		this.chara = chara;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}
