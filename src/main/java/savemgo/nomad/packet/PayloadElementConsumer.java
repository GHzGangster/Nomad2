package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface PayloadElementConsumer {

	void accept(ByteBuf payload, Integer index) throws Exception;

}