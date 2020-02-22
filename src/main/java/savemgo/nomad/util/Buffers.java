package savemgo.nomad.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import savemgo.nomad.packet.PayloadGroupConsumer;
import savemgo.nomad.packet.PayloadGroup;

public class Buffers {

	public static final ByteBufAllocator ALLOCATOR = PooledByteBufAllocator.DEFAULT;

	public static void release(ByteBuf buffer) {
		if (buffer != null && buffer.refCnt() > 0) {
			buffer.release();
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
			PayloadGroupConsumer consumer) throws Exception {
		int full = elementCount / maxElements;
		int partial = elementCount % maxElements;

		int total = full + (partial > 0 ? 1 : 0);
		var buffers = new ByteBuf[total];
		int elem = 0;

		int i = 0;
		for (; i < full; i++) {
			var buffer = Buffers.ALLOCATOR.directBuffer(elementSize * maxElements);
			buffers[i] = buffer;

			for (int j = 0; j < maxElements; j++) {
				int writerIndex = buffer.writerIndex();
				consumer.accept(buffer, elem++);
				buffer.writerIndex(writerIndex + elementSize);
			}
		}

		if (partial > 0) {
			var buffer = Buffers.ALLOCATOR.directBuffer(elementSize * partial);
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

	public static String readString(ByteBuf buffer, int maxLength) {
		return readString(buffer, maxLength, CharsetUtil.ISO_8859_1);
	}

	public static String readString(ByteBuf buffer, int maxLength, Charset charset) {
		int indexStart = buffer.readerIndex();
		int readLength = Math.min(buffer.readableBytes(), maxLength);

		int stringLength = readLength;
		for (int i = 0; i < readLength; i++) {
			int val = (int) buffer.readByte();
			if (val == 0x00) {
				stringLength = i;
				break;
			}
		}

		buffer.readerIndex(indexStart + maxLength);
		
		if (stringLength > 0) {
			return buffer.toString(indexStart, stringLength, charset);
		}
		return "";
	}

	public static void writeStringFill(ByteBuf buffer, String str, int length) throws CharacterCodingException {
		writeStringFill(buffer, str, length, CharsetUtil.ISO_8859_1);
	}

	public static void writeStringFill(ByteBuf buffer, String str, int length, Charset charset)
			throws CharacterCodingException {
		var byteBuffer = ByteBuffer.allocate(length);
		var charBuffer = CharBuffer.wrap(str);

		var ce = charset.newEncoder();
		ce.encode(charBuffer, byteBuffer, true);
		byteBuffer.position(0);

		buffer.writeBytes(byteBuffer);
	}

	public static void moveReadableToStart(ByteBuf buffer) {
		int length = buffer.readableBytes();
		int longCount = length >>> 3;
		int byteCount = length & 7;

		int readerIndex = buffer.readerIndex();
		buffer.setIndex(0, 0);

		for (int i = longCount; i > 0; i--) {
			long l = buffer.getLong(readerIndex);
			buffer.writeLong(l);
			readerIndex += 8;
		}

		for (int i = byteCount; i > 0; i--) {
			byte b = buffer.getByte(readerIndex);
			buffer.writeByte(b);
			readerIndex++;
		}
	}

}
