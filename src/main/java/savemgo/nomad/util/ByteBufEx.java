package savemgo.nomad.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;

public class ByteBufEx {

	private static final Recycler<ByteBufEx> RECYCLER = new Recycler<>() {
		@Override
		protected ByteBufEx newObject(Handle<ByteBufEx> handle) {
			return new ByteBufEx(handle);
		}
	};

	public static ByteBufEx get(ByteBuf buffer) {
		var bufferEx = RECYCLER.get();
		bufferEx.setBuffer(buffer);
		return bufferEx;
	}

	private final Handle<ByteBufEx> handle;

	private ByteBuf buffer;

	private ByteBufEx(Handle<ByteBufEx> handle) {
		this.handle = handle;
	}

	public void recycle() {
		this.buffer = null;
		handle.recycle(this);
	}

	public String readString(int maxLength) {
		return readString(maxLength, CharsetUtil.ISO_8859_1);
	}

	public String readString(int maxLength, Charset charset) {
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

		if (stringLength > 0) {
			return buffer.toString(indexStart, stringLength, charset);
		}
		return "";
	}

	public ByteBufEx writeStringFill(String str, int length) throws CharacterCodingException {
		return writeStringFill(str, length, CharsetUtil.ISO_8859_1);
	}

	public ByteBufEx writeStringFill(String str, int length, Charset charset) throws CharacterCodingException {
		var byteBuffer = ByteBuffer.allocate(length);
		var charBuffer = CharBuffer.wrap(str);

		var ce = charset.newEncoder();
		ce.encode(charBuffer, byteBuffer, true);
		byteBuffer.position(0);

		buffer.writeBytes(byteBuffer);

		return this;
	}

	public static void moveReadableToStart(ByteBuf buffer) {

	}

	public ByteBufEx moveReadableToStart() {
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

		return this;
	}

	public ByteBuf getBuffer() {
		return buffer;
	}

	public void setBuffer(ByteBuf buffer) {
		this.buffer = buffer;
	}

}
