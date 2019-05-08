package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;

public class Packet {

	private short command;
	private ByteBuf payload;

	public Packet() {

	}

	public Packet(int command) {
		this.command = (short) command;
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

}
