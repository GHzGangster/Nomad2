package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.record.Chara;
import savemgo.nomad.database.record.CharaAppearance;
import savemgo.nomad.database.record.User;
import savemgo.nomad.local.LocalCharacter;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.local.LocalUser;
import savemgo.nomad.local.util.LocalUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PacketError;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Users {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet GETSESSION_OK = new Packet(0x3004, 0);

	public static void getSession(ChannelHandlerContext ctx, Packet in, boolean isAccountLobby, LocalLobby localLobby) {
		PacketError error = null;
		try (var handle = DB.open()) {
			var bi = in.getPayload();

			// Read id
			int id = bi.readInt();

			// Read session id
			// TODO: Use 16 char session ids in gidauth, and don't trim here
			Ptsys.encryptBlowfish(Packets.KEY_KIT, bi, 0x4, bi, 0x4, 0x10);
			var sessionId = Buffers.readString(bi, 0x10);
			sessionId = sessionId.substring(0, 8);
			logger.debug("Session id: {}", sessionId);

			// Get user and/or character by session id
			User user = null;
			Chara chara = null;
			if (isAccountLobby) {
				user = handle.createQuery("""
						SELECT id, username, role, banned_until, system, slots
						FROM users
						WHERE id=:id AND session=:sessionId
						""").bind("id", id).bind("sessionId", sessionId).mapToBean(User.class).findOne().orElse(null);
			} else {
				handle.registerRowMapper(BeanMapper.factory(User.class, "u"));
				handle.registerRowMapper(BeanMapper.factory(Chara.class, "c"));
				handle.registerRowMapper(JoinRowMapper.forTypes(User.class, Chara.class));

				var row = handle.createQuery("""
						SELECT u.id u_id, u.username u_username, u.role u_role, u.banned_until u_banned_until,
						u.system u_system, u.slots u_slots,
						c.id c_id, c.user c_user, c.name c_name, c.old_name c_old_name, c.rank c_rank,
						c.comment c_comment, c.gameplay_options c_gameplay_options, c.active c_active,
						c.creation_time c_creation_time, c.lobby c_lobby
						FROM users u JOIN mgo2_charas c ON c.user=u.id
						WHERE u.session=:sessionId AND c.id=:id AND c.active=1
						""").bind("id", id).bind("sessionId", sessionId).mapTo(JoinRow.class).findOne().orElse(null);
				if (row != null) {
					user = row.get(User.class);
					chara = row.get(Chara.class);
				}
			}

			if (user == null) {
				logger.error("getSession- Invalid session: {}", sessionId);
				error = PacketError.INVALID_SESSION;
				return;
			}

			// TODO: Check banned_until

			// Start session on server
			onLobbyJoin(ctx.channel(), localLobby, user, chara);

			ctx.write(GETSESSION_OK);
		} catch (Exception e) {
			logger.error("getSession- Exception occurred.", e);
			error = PacketError.GENERAL;
		} finally {
			Packets.writeError(ctx, 0x3004, error);
		}
	}

	private static final byte[] CHARACTERLIST_UNK = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07,
			(byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public static void getCharacterList(ChannelHandlerContext ctx) {
		PacketError error = null;
		ByteBuf bo = null;
		try (var handle = DB.open()) {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getCharacterList- Invalid session.");
				error = PacketError.INVALID_SESSION;
				return;
			}

			// Get character and appearance
			handle.registerRowMapper(BeanMapper.factory(Chara.class, "c"));
			handle.registerRowMapper(BeanMapper.factory(CharaAppearance.class, "a"));
			handle.registerRowMapper(JoinRowMapper.forTypes(Chara.class, CharaAppearance.class));

			var rows = handle.createQuery("""
					SELECT c.id c_id, c.name c_name, c.name_prefix c_name_prefix,
					a.gender a_gender, a.face a_face, a.voice a_voice, a.voice_pitch a_voice_pitch,
					a.head a_head, a.head_color a_head_color, a.upper a_upper, a.upper_color a_upper_color,
					a.lower a_lower, a.lower_color a_lower_color, a.chest a_chest, a.chest_color a_chest_color,
					a.waist a_waist, a.waist_color a_waist_color, a.hands a_hands, a.hands_color a_hands_color,
					a.feet a_feet, a.feet_color a_feet_color, a.accessory1 a_accessory1,
					a.accessory1_color a_accessory1_color, a.accessory2 a_accessory2,
					a.accessory2_color a_accessory2_color, a.face_paint a_face_paint
					FROM mgo2_charas c JOIN mgo2_charas_appearance a ON a.chara=c.id
					WHERE c.user=:user AND c.active=1
					""").bind("user", user.getId()).mapTo(JoinRow.class).list();

			int numCharacters = rows.size();

			// Create payload
			bo = Buffers.ALLOCATOR.directBuffer(0x1d7);
			bo.writeInt(0).writeByte(user.getSlots()).writeByte(numCharacters).writeZero(1);

			// Write characters
			for (int i = 0; i < rows.size(); i++) {
				var row = rows.get(i);
				var chara = row.get(Chara.class);
				var appearance = row.get(CharaAppearance.class);

				if (i == 0) {
					Buffers.writeStringFill(bo, Util.getFullCharacterName(chara), 16);
					bo.writeZero(1);
				} else {
					bo.writeInt(i);
				}

				bo.writeInt(chara.getId());
				Buffers.writeStringFill(bo, Util.getFullCharacterName(chara), 16);
				bo.writeByte(appearance.getGender()).writeByte(appearance.getFace()).writeByte(appearance.getUpper())
						.writeByte(appearance.getLower()).writeByte(appearance.getFacePaint())
						.writeByte(appearance.getUpperColor()).writeByte(appearance.getLowerColor())
						.writeByte(appearance.getVoice()).writeByte(appearance.getVoicePitch()).writeZero(4)
						.writeByte(appearance.getHead()).writeByte(appearance.getChest())
						.writeByte(appearance.getHands()).writeByte(appearance.getWaist())
						.writeByte(appearance.getFeet()).writeByte(appearance.getAccessory1())
						.writeByte(appearance.getAccessory2()).writeByte(appearance.getHeadColor())
						.writeByte(appearance.getChestColor()).writeByte(appearance.getHandsColor())
						.writeByte(appearance.getWaistColor()).writeByte(appearance.getFeetColor())
						.writeByte(appearance.getAccessory1Color()).writeByte(appearance.getAccessory2Color())
						.writeZero(1);
			}

			// Write unknown bytes. Affects ability to create female characters.
			bo.writeZero(0x1b4 - bo.writerIndex());
			bo.writeBytes(CHARACTERLIST_UNK);

			// Write payload
			ctx.write(new Packet(0x3049, bo));
		} catch (Exception e) {
			logger.error("getCharacterList- Exception occurred.", e);
			error = PacketError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x3049, error);
		}
	}

	private static final Packet SELECTCHARACTER_OK = new Packet(0x3104, 0);

	public static void selectCharacter(ChannelHandlerContext ctx, Packet in) {
		PacketError error = null;
		ByteBuf bo = null;
		try (var handle = DB.open()) {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("selectCharacter- Invalid session.");
				error = PacketError.INVALID_SESSION;
				return;
			}

			var bi = in.getPayload();

			// Read character index
			int index = bi.readByte();

			// Get characters
			var characters = handle.createQuery("""
					SELECT id
					FROM mgo2_charas
					WHERE user=:user AND active=1 
					""").bind("user", user.getId()).mapToBean(Chara.class).list();

			int numCharacters = characters.size();
			if (index < 0 || index > numCharacters - 1) {
				logger.error("selectCharacter- Index out of bounds.");
				error = PacketError.GENERAL;
				return;
			}

			var character = characters.get(index);
			if (!character.isActive()) {
				logger.error("selectCharacter- Character not active.");
				error = PacketError.GENERAL;
				return;
			}

			ctx.write(SELECTCHARACTER_OK);
		} catch (Exception e) {
			logger.error("selectCharacter- Exception occurred.", e);
			error = PacketError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x3104, error);
		}
	}

	public static void onLobbyJoin(Channel channel, LocalLobby localLobby, User user, Chara chara) {
		var localUser = new LocalUser();
		localUser.setId(user.getId());
		localUser.setUsername(user.getUsername());
		localUser.setRole(user.getRole());
		localUser.setSystem(user.getSystem());
		localUser.setSlots(user.getSlots());

		if (chara != null) {
			var localCharacter = new LocalCharacter();
			localUser.setCharacter(localCharacter);

			localCharacter.setUser(localUser);
			localCharacter.setLobby(localLobby);

			localCharacter.setId(chara.getId());
			localCharacter.setName(chara.getName());
			localCharacter.setNamePrefix(chara.getNamePrefix());
			localCharacter.setActive(chara.isActive());
		}

		LocalUsers.add(channel, localUser);
	}

	public static void onLobbyLeave(Channel channel) {
		LocalUsers.remove(channel);
	}

}
