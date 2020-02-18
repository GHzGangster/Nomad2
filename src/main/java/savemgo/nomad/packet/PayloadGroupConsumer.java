package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface PayloadGroupConsumer {

	void accept(ByteBuf payload, Integer index) throws Exception;

}