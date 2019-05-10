package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;

public class Packet {

	private short command;

	private int result;
	private ByteBuf payload;

	public Packet() {

	}

	public Packet(int command) {
		this.command = (short) command;
	}

	public Packet(int command, int result) {
		this.command = (short) command;
		this.result = result;
	}

	public Packet(int command, ByteBuf payload) {
		this.command = (short) command;
		this.payload = payload;
	}

	public int getCommand() {
		return command;
	}

	public void setCommand(int command) {
		this.command = (short) command;
	}

	public ByteBuf getPayload() {
		return payload;
	}

	public void setPayload(ByteBuf payload) {
		this.payload = payload;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}
	
}
