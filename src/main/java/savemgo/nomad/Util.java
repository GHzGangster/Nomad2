package savemgo.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadElementConsumer;
import savemgo.nomad.packet.PayloadGroup;

public class Util {

	public static final ByteBufAllocator ALLOCATOR = PooledByteBufAllocator.DEFAULT;
	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static void release(ByteBuf buffer) {
		if (buffer != null) {
			buffer.release();
		}
	}

	public static void release(Packet packet) {
		if (packet != null) {
			release(packet.getPayload());
		}
	}

	public static void release(PayloadGroup payloads) {
		if (payloads != null) {
			for (var buffer : payloads.getBuffers()) {
				release(buffer);
			}
		}
	}

	public static PayloadGroup createPayloads(int elementCount, int elementSize, int maxElements,
			PayloadElementConsumer consumer) throws Exception {
		int full = elementCount / maxElements;
		int partial = elementCount % maxElements;

		int total = full + (partial > 0 ? 1 : 0);
		var buffers = new ByteBuf[total];
		int elem = 0;

		int i = 0;
		for (; i < full; i++) {
			var buffer = Util.ALLOCATOR.directBuffer(elementSize * maxElements);
			buffers[i] = buffer;

			for (int j = 0; j < maxElements; j++) {
				int writerIndex = buffer.writerIndex();
				consumer.accept(buffer, elem++);
				buffer.writerIndex(writerIndex + elementSize);
			}
		}

		if (partial > 0) {
			var buffer = Util.ALLOCATOR.directBuffer(elementSize * partial);
			buffers[i] = buffer;

			for (int j = 0; j < partial; j++) {
				int writerIndex = buffer.writerIndex();
				consumer.accept(buffer, elem++);
				buffer.writerIndex(writerIndex + elementSize);
			}
		}

		var payloads = new PayloadGroup();
		payloads.setBuffers(buffers);
		return payloads;
	}

}
