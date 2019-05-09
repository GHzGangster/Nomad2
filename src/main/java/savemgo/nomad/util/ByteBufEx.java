package savemgo.nomad.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class ByteBufEx {

	private ByteBuf buffer;

	public ByteBufEx(ByteBuf buffer) {
		this.buffer = buffer;
	}

	public String readString(int maxLength) {
		return readString(maxLength, CharsetUtil.ISO_8859_1);
	}

	public String readString(int maxLength, Charset charset) {
		int indexStart = buffer.readerIndex();
		int readLength = Math.min(buffer.readableBytes(), maxLength);

		int stringLength = 0;
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

	public ByteBufEx writeStringFill(String str, int length, Charset charset)
			throws CharacterCodingException {
		var byteBuffer = ByteBuffer.allocate(length);
		var charBuffer = CharBuffer.wrap(str);

		var ce = charset.newEncoder();
		ce.encode(charBuffer, byteBuffer, true);
		byteBuffer.position(0);

		buffer.writeBytes(byteBuffer);
		
		return this;
	}

	public ByteBuf getBuffer() {
		return buffer;
	}

	public void setBuffer(ByteBuf buffer) {
		this.buffer = buffer;
	}

}
