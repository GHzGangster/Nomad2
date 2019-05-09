package savemgo.nomad.packet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.Recycler;
import savemgo.nomad.Constants;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.ByteBufEx;
import savemgo.nomad.util.Packets;

@Sharable
public class PacketDecoder extends ChannelInboundHandlerAdapter {
	
	private static final Logger logger = LogManager.getLogger();

	private static final AttributeKey<ByteBuf> BUFFER_IN = AttributeKey.valueOf("bufferIn");
	private static final AttributeKey<Integer> SEQUENCE_IN = AttributeKey.valueOf("sequenceIn");

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		var buffer = ctx.alloc().buffer(Constants.SERVER_CLIENT_RCVBUF);
		var attr = ctx.channel().attr(BUFFER_IN);
		attr.set(buffer);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		var attr = ctx.channel().attr(BUFFER_IN);
		var buffer = attr.get();
		attr.set(null);
		Buffers.release(buffer);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf message = (ByteBuf) msg;
		try {
			var attrBuffer = ctx.channel().attr(BUFFER_IN);
			var buffer = attrBuffer.get();
			if (buffer == null) {
				return;
			}

			buffer.writeBytes(message);
			
//			logger.debug("Buffer start {} {}\n{}", buffer.readerIndex(), buffer.writerIndex(), ByteBufUtil.prettyHexDump(buffer));

			while (buffer.isReadable()) {
//				logger.debug("Buffer loop {} {}\n{}", buffer.readerIndex(), buffer.writerIndex(), ByteBufUtil.prettyHexDump(buffer));
				
				// Check for the header
				int readable = buffer.readableBytes();
				if (readable < Constants.PACKET_HEADER) {
					logger.debug("Packet is too short, waiting for header...");
					break;
				}

				// Get the payload length
				int readerIndex = buffer.readerIndex();

				int command = buffer.getUnsignedShort(readerIndex) ^ Constants.PACKET_SCRAMBLER_HIGH;
				int length = buffer.getUnsignedShort(readerIndex + 2) ^ Constants.PACKET_SCRAMBLER_LOW;

				// TODO: Check pad
				int pad = 0;

				// Check the payload legth
				int payloadLength = length + pad;
				if (Constants.PACKET_PAYLOAD_MAX < payloadLength) {
					logger.debug("Payload isn't a valid length: {}", payloadLength);
					break;
				}

				// Check for the payload
				int totalLength = Constants.PACKET_HEADER + payloadLength;
				if (readable < totalLength) {
					logger.debug("Packet is too short, waiting for payload...");
					break;
				}

				// Xor the packet, and advance the reader index
				Packets.xorScrambler(buffer, readerIndex, totalLength);
				buffer.readerIndex(readerIndex + totalLength);
				
				// TODO: Get seq and digest, and check them

				// Get payload
				ByteBuf payload = null;
				try {
					payload = buffer.copy(readerIndex + Constants.PACKET_HEADER, payloadLength);

					// TODO: Decrypt the payload if needed, and chop off the extra bytes
				} catch (Exception e) {
					Buffers.release(payload);
					throw e;
				}				

				// Create packet
				var packet = new Packet();
				packet.setCommand(command);
				if (payload != null) {
					packet.setPayload(payload);
				}

				logger.printf(Level.DEBUG, "Received %04x", packet.getCommand());
				if (packet.getPayload() != null) {
					logger.debug(ByteBufUtil.hexDump(packet.getPayload()));
				}

				// Pass the packet
				ctx.fireChannelRead(packet);
			}
			
//			logger.debug("Buffer end {} {}\n{}", buffer.readerIndex(), buffer.writerIndex(), ByteBufUtil.prettyHexDump(buffer));
			
			// Reset the buffer
			var bufferEx = ByteBufEx.get(buffer);
			try {
				bufferEx.moveReadableToStart();
			} finally {
				bufferEx.recycle();
			}
			
//			logger.debug("Buffer end reset {} {}\n{}", buffer.readerIndex(), buffer.writerIndex(), ByteBufUtil.prettyHexDump(buffer));
		} catch (Exception e) {
			logger.error("Failed to decode packet.", e);
		} finally {
			Buffers.release(message);
		}
	}

}
