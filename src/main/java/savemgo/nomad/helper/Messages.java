package savemgo.nomad.helper;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.local.util.LocalUsers;
import savemgo.nomad.packet.GameError;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;

public class Messages {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet INBOX_START = new Packet(0x4821, 0);
	private static final Packet INBOX_END = new Packet(0x4823, 0);

	public static void getMessages(ChannelHandlerContext ctx, Packet in, int forceType) {
		GameError error = null;
		PayloadGroup payloads = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getInbox- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			// Create payload
			payloads = Buffers.createPayloads(1, (i) -> {
				var bo = ctx.alloc().directBuffer(266);

				/**
				 * Mtype
				 * 
				 * 0 = Inbox 1 = Sent 3 = Announcement
				 */

				int box = 3;
				boolean important = false;
				boolean read = false;

				int unk1 = 1;
				int unk2 = 0;

				int time = (int) Instant.now().getEpochSecond();
				String senderName = "SaveMGO";
				String message = "This is a test!";

				bo.writeByte(box).writeByte(i).writeByte(unk1);
				Buffers.writeStringFill(bo, senderName, 128);
				Buffers.writeStringFill(bo, message, 128);
				bo.writeInt(time).writeByte(unk2).writeBoolean(important).writeBoolean(read);

				return bo;
			});

			// Write payload
			ctx.write(INBOX_START);
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(0x4822, payload));
			}
			ctx.write(INBOX_END);
		} catch (Exception e) {
			logger.error("getInbox- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(payloads);
		} finally {
			Packets.writeError(ctx, 0x4821, error);
		}
	}

	/**
	 * 4800/4801 Errors
	 * 
	 * -0x321 Improper address entered. <br>
	 * -0x322 Receiver's mailbox is full. <br>
	 * -0x32a You are on the receiver's Block List. <br>
	 * -0x334 Improper address entered. <br>
	 * -0x33e The receiver has blocked incoming mail. <br>
	 * -0x33f You are currently unable to use mail services. <br>
	 * -0x340 Mail subject contains an invalid word. <br>
	 * -0x341 Mail message contains an invalid word. <br>
	 * -0x4ce You are currently banned from using clan services. <br>
	 */
	public static void send(ChannelHandlerContext ctx, Packet in) {

	}

}
