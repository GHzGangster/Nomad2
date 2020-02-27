package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface PayloadGroupIntFunction {

	ByteBuf apply(Integer index) throws Exception;

}