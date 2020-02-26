package savemgo.nomad.database.record;

public class CharaChatMacro {

	private int id;
	private int chara;
	private int index;
	private String text;

	public CharaChatMacro() {

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

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
