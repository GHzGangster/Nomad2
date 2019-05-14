package savemgo.nomad.session;

public class NomadUser {

	private int id;
	private String username;
	private int role;
	private Integer bannedUntil;
	private int isCfw;
	private Integer chara;

	public NomadUser() {

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

	public int getIsCfw() {
		return isCfw;
	}

	public void setIsCfw(int isCfw) {
		this.isCfw = isCfw;
	}

	public Integer getChara() {
		return chara;
	}

	public void setChara(Integer chara) {
		this.chara = chara;
	}

	public Integer getBannedUntil() {
		return bannedUntil;
	}

	public void setBannedUntil(Integer bannedUntil) {
		this.bannedUntil = bannedUntil;
	}

}
