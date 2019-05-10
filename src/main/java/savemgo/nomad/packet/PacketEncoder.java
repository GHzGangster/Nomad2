package savemgo.nomad.packet;

import javax.crypto.Mac;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.primitives.Ints;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Constants;
import savemgo.nomad.util.Packets;

@Sharable
public class PacketEncoder extends ChannelOutboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger();

	private static final AttributeKey<Integer> SEQUENCE_OUT = AttributeKey.valueOf("sequenceOut");

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// Prepare channel sequence
		var attrSequence = ctx.channel().attr(SEQUENCE_OUT);
		attrSequence.set(1);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		var packet = (Packet) msg;
		ByteBuf buffer = null;
		try {
			logger.printf(Level.DEBUG, "Sent %04x", packet.getCommand());
			if (packet.getPayload() != null) {
				logger.debug(ByteBufUtil.hexDump(packet.getPayload()));
			}

			// Get command
			int command = packet.getCommand();

			// Get original payload length
			int length = 0;
			if (packet.getPayload() != null) {
				length = packet.getPayload().readableBytes();
			}

			// Figure out if payload should be encrypted, and figure out the padding
			int padding = 0;
			boolean shouldEncrypt = Ints.contains(Constants.PACKET_CRYPTED_OUT, command);
			if (shouldEncrypt && length > 0) {
				padding = 8 - (length % 8);
			}

			// Get payload length
			int payloadLength = length + padding;

			// Get sequence
			var attrSequence = ctx.channel().attr(SEQUENCE_OUT);
			int sequence = attrSequence.get();
			
			// Create buffer
			int totalLength = 0x18 + payloadLength;
			buffer = Buffers.ALLOCATOR.directBuffer(totalLength);

			// Write header
			buffer.writeShort(command).writeShort(length).writeInt(sequence);

			// Write digest placeholder
			buffer.writeZero(0x10);

			// Write payload
			if (payloadLength > 0) {
				buffer.writeBytes(packet.getPayload());
				if (padding > 0) {
					buffer.writeZero(padding);
				}
			}

			// Encrypt the payload if necessary
			if (payloadLength > 0 && shouldEncrypt) {
				Ptsys.encryptBlowfishSimple(Constants.PACKET_MIO, buffer, 0x18, buffer, 0x18, payloadLength);
			}
			
			// Calculate digest
			var mac = Mac.getInstance("HmacMD5");
			mac.init(Constants.PACKET_HMAC_SPEC);

			var buf = buffer.nioBuffer(0, 8);
			mac.update(buf);
			if (payloadLength > 0) {
				buf = buffer.nioBuffer(0x18, payloadLength);
				mac.update(buf);
			}

			var digest = mac.doFinal();

			// Write digest
			buffer.setBytes(0x8, digest);
			
			// Xor the packet
			Packets.scramble(buffer, 0, totalLength);
			
			// Send the buffer
			ctx.write(buffer, ctx.voidPromise());
			
			// Increment sequence
			attrSequence.set(++sequence);
		} catch (Exception e) {
			logger.error("Failed to encode packet.", e);
			Buffers.release(buffer);
		} finally {
			Buffers.release(packet.getPayload());
		}
	}

}
