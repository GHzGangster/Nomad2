package savemgo.nomad.database.record;

public class ClanMember {

	private int id;
	private int clan;
	private int chara;

	public ClanMember() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getClan() {
		return clan;
	}

	public void setClan(int clan) {
		this.clan = clan;
	}

	public int getChara() {
		return chara;
	}

	public void setChara(int chara) {
		this.chara = chara;
	}

}
