package savemgo.nomad.local;

public class LocalUser {

	// TODO: Make sure that we only really store things that we need to use more
	// than once, and doesn't change often.

	private int id;
	private String username;
	private int role;
	private int system;
	private LocalChara character;
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

	public LocalChara getCharacter() {
		return character;
	}

	public void setCharacter(LocalChara character) {
		this.character = character;
	}

	public int getSlots() {
		return slots;
	}

	public void setSlots(int slots) {
		this.slots = slots;
	}

}
