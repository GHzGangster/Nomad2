package savemgo.nomad.local;

public class LocalUser {

	private int id;
	private String username;
	private int role;
	private int system;
	private Integer chara;
	private int slots;

	public LocalUser() {

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

	public int getSystem() {
		return system;
	}

	public void setSystem(int system) {
		this.system = system;
	}

	public Integer getChara() {
		return chara;
	}

	public void setChara(Integer chara) {
		this.chara = chara;
	}

	public int getSlots() {
		return slots;
	}

	public void setSlots(int slots) {
		this.slots = slots;
	}

}
