package savemgo.nomad.database.record;

public class CharaBlocked {

	private int id;
	private int chara;
	private int target;

	public CharaBlocked() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getChara() {
		return chara;
	}

	public void setChara(int chara) {
		this.chara = chara;
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

}
