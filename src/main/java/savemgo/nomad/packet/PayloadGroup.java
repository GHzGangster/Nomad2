package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;

public class PayloadGroup {

	private ByteBuf[] buffers;

	public PayloadGroup() {

	}

	public ByteBuf[] getBuffers() {
		return buffers;
	}

	public void setBuffers(ByteBuf[] buffers) {
		this.buffers = buffers;
	}

}
