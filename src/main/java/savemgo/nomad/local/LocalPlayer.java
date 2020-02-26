package savemgo.nomad.local;

public class LocalPlayer {

	private LocalGame game;
	private LocalChara chara;
	private int team;
	private int ping;

	public LocalPlayer() {

	}

	public LocalGame getGame() {
		return game;
	}

	public void setGame(LocalGame game) {
		this.game = game;
	}

	public LocalChara getChara() {
		return chara;
	}

	public void setChara(LocalChara chara) {
		this.chara = chara;
	}

	public int getTeam() {
		return team;
	}

	public void setTeam(int team) {
		this.team = team;
	}

	public int getPing() {
		return ping;
	}

	public void setPing(int ping) {
		this.ping = ping;
	}

}
