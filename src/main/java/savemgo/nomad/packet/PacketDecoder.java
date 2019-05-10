package savemgo.nomad.packet;

import java.util.Arrays;

import javax.crypto.Mac;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.primitives.Ints;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Constants;
import savemgo.nomad.util.Packets;

@Sharable
public class PacketDecoder extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger();

	private static final AttributeKey<ByteBuf> BUFFER_IN = AttributeKey.valueOf("bufferIn");
	private static final AttributeKey<Integer> SEQUENCE_IN = AttributeKey.valueOf("sequenceIn");

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		// Prepare channel buffer
		var buffer = ctx.alloc().buffer(Constants.SERVER_CLIENT_RCVBUF);
		var attrBuffer = ctx.channel().attr(BUFFER_IN);
		attrBuffer.set(buffer);

		// Prepare channel sequence
		var attrSequence = ctx.channel().attr(SEQUENCE_IN);
		attrSequence.set(1);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		// Destory channel buffer
		var attrBuffer = ctx.channel().attr(BUFFER_IN);
		var buffer = attrBuffer.get();
		attrBuffer.set(null);
		Buffers.release(buffer);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		var message = (ByteBuf) msg;
		try {
			var attrBuffer = ctx.channel().attr(BUFFER_IN);
			var buffer = attrBuffer.get();

			buffer.writeBytes(message);

			while (buffer.isReadable()) {
				// Check for the header
				int readable = buffer.readableBytes();
				if (readable < 0x18) {
					logger.debug("Packet is too short, waiting for header...");
					break;
				}

				// Get the original payload length
				int readerIndex = buffer.readerIndex();

				int command = buffer.getUnsignedShort(readerIndex) ^ Constants.PACKET_SCRAMBLER_HIGH;
				int length = buffer.getUnsignedShort(readerIndex + 0x2) ^ Constants.PACKET_SCRAMBLER_LOW;

				// Check if payload is encrypted, if so figure out the padding
				boolean encrypted = Ints.contains(Constants.PACKET_CRYPTED_IN, command);
				int padding = 0;
				if (encrypted && (length % 8) > 0) {
					padding = 8 - (length % 8);
				}

				// Check the payload legth
				int payloadLength = length + padding;
				if (Constants.PACKET_PAYLOAD_MAX < payloadLength) {
					logger.debug("Payload isn't a valid length: {}", payloadLength);
					break;
				}

				// Check for the payload
				int totalLength = 0x18 + payloadLength;
				if (readable < totalLength) {
					logger.debug("Packet is too short, waiting for payload...");
					break;
				}

				// Xor the packet, and advance the reader index
				Packets.scramble(buffer, readerIndex, totalLength);
				buffer.readerIndex(readerIndex + totalLength);

				// Get sequence
				int sequence = buffer.getInt(readerIndex + 0x4);

				// Get our sequence and check
				var attrSequence = ctx.channel().attr(SEQUENCE_IN);
				int ourSequence = attrSequence.get();
				if (sequence != ourSequence) {
					logger.warn("Sequence is incorrect!");
//					continue;
				}

				// Increment sequence
				attrSequence.set(++ourSequence);

				// Get digest
				var digest = new byte[0x10];
				buffer.getBytes(readerIndex + 0x8, digest);

				// Calculate our digest and check
				var mac = Mac.getInstance("HmacMD5");
				mac.init(Constants.PACKET_HMAC_SPEC);

				var buf = buffer.nioBuffer(readerIndex, 8);
				mac.update(buf);
				if (payloadLength > 0) {
					buf = buffer.nioBuffer(readerIndex + 0x18, payloadLength);
					mac.update(buf);
				}

				var ourDigest = mac.doFinal();
				if (!Arrays.equals(digest, ourDigest)) {
					logger.warn("Digest is incorrect!");
					continue;
				}

				// Get payload
				ByteBuf payload = null;
				if (payloadLength > 0) {
					try {
						payload = buffer.copy(readerIndex + 0x18, payloadLength);

						// Decrypt payload if needed
						if (encrypted) {
							Ptsys.decryptBlowfishSimple(Constants.PACKET_MIO, payload, 0, payload, 0, payloadLength);
							payload.writerIndex(length);
						}
					} catch (Exception e) {
						Buffers.release(payload);
						throw e;
					}
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

			// Reset the buffer
			Buffers.moveReadableToStart(buffer);
		} catch (Exception e) {
			logger.error("Failed to decode packet.", e);
		} finally {
			Buffers.release(message);
		}
	}

}
