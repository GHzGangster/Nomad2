package savemgo.nomad.database.record;

public class Clan {

	private int id;
	private String name;
	private int leader;
	private String comment;
	private String notice;
	private int noticeTime;
	private int noticeWriter;
	private int emblemEditor;
	private byte[] emblem;
	private byte[] emblemWip;
	private int open;

	public Clan() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLeader() {
		return leader;
	}

	public void setLeader(int leader) {
		this.leader = leader;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getNotice() {
		return notice;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}

	public int getNoticeTime() {
		return noticeTime;
	}

	public void setNoticeTime(int noticeTime) {
		this.noticeTime = noticeTime;
	}

	public int getNoticeWriter() {
		return noticeWriter;
	}

	public void setNoticeWriter(int noticeWriter) {
		this.noticeWriter = noticeWriter;
	}

	public int getEmblemEditor() {
		return emblemEditor;
	}

	public void setEmblemEditor(int emblemEditor) {
		this.emblemEditor = emblemEditor;
	}

	public byte[] getEmblem() {
		return emblem;
	}

	public void setEmblem(byte[] emblem) {
		this.emblem = emblem;
	}

	public byte[] getEmblemWip() {
		return emblemWip;
	}

	public void setEmblemWip(byte[] emblemWip) {
		this.emblemWip = emblemWip;
	}

	public int getOpen() {
		return open;
	}

	public void setOpen(int open) {
		this.open = open;
	}

}
