package savemgo.nomad.helper;

import java.time.Instant;

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
import savemgo.nomad.database.record.CharaSkills;
import savemgo.nomad.database.record.User;
import savemgo.nomad.local.LocalChara;
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
			String sessionId = Buffers.readString(bi, 0x10);
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
					a.feet a_feet, a.feet_color a_feet_color, a.accessory_1 a_accessory_1,
					a.accessory_1_color a_accessory_1_color, a.accessory_2 a_accessory_2,
					a.accessory_2_color a_accessory_2_color, a.face_paint a_face_paint
					FROM mgo2_charas c JOIN mgo2_charas_appearance a ON a.chara=c.id
					WHERE c.user=:user AND c.active=1
					""").bind("user", user.getId()).mapTo(JoinRow.class).list();

			int numCharacters = rows.size();

			// Create payload
			bo = ctx.alloc().directBuffer(0x1d7);
			bo.writeInt(0).writeByte(user.getSlots()).writeByte(numCharacters).writeZero(1);

			// Write characters
			for (int i = 0; i < numCharacters; i++) {
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

			ctx.write(SELECTCHARACTER_OK);
		} catch (Exception e) {
			logger.error("selectCharacter- Exception occurred.", e);
			error = PacketError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x3104, error);
		}
	}

	private static final String CREATECHARACTER_GAMEPLAYOPTIONS = """
		{
			"onlineStatusMode": 0,
			"emailFriendsOnly": false,
			"receiveNotices": true,
			"receiveInvites": true,
			"normalViewVerticalInvert": false,
			"normalViewHorizontalInvert": false,
			"normalViewSpeed": 5,
			"shoulderViewVerticalInvert": false,
			"shoulderViewHorizontalInvert": false,
			"shoulderViewSpeed": 5,
			"firstViewVerticalInvert": false,
			"firstViewHorizontalInvert": false,
			"firstViewSpeed": 5,
			"firstViewPlayerDirection": true,
			"viewChangeSpeed": 5,
			"firstViewMemory": false,
			"radarLockNorth": false,
			"radarFloorHide": false,
			"hudDisplaySize": 0,
			"hudHideNameTags": false,
			"lockOnEnabled": false,
			"weaponSwitchMode": 2,
			"weaponSwitchA": 0,
			"weaponSwitchB": 1,
			"weaponSwitchC": 2,
			"weaponSwitchNow": 0,
			"weaponSwitchBefore": 1,
			"itemSwitchMode": 2,
			"codec1Name": "",
			"codec1a": 1,
			"codec1b": 3,
			"codec1c": 4,
			"codec1d": 2,
			"codec2Name": "",
			"codec2a": 10,
			"codec2b": 12,
			"codec2c": 13,
			"codec2d": 11,
			"codec3Name": "",
			"codec3a": 14,
			"codec3b": 16,
			"codec3c": 17,
			"codec3d": 15,
			"codec4Name": "",
			"codec4a": 5,
			"codec4b": 7,
			"codec4c": 8,
			"codec4d": 6,
			"voiceChatRecognitionLevel": 5,
			"voiceChatVolume": 5,
			"headsetVolume": 5,
			"bgmVolume": 10
		}
		""";

	public static void createCharacter(ChannelHandlerContext ctx, Packet in) {
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

			String name = Buffers.readString(bi, 16);

			int gender = bi.readByte();
			int face = bi.readByte();
			int upper = bi.readByte();
			int lower = 22;
			bi.skipBytes(1);
			int facePaint = bi.readByte();
			int upperColor = bi.readByte();
			int lowerColor = bi.readByte();
			int voice = bi.readByte();
			int voicePitch = bi.readByte();
			bi.skipBytes(4);
			int head = bi.readByte();
			int chest = bi.readByte();
			int hands = bi.readByte();
			int waist = bi.readByte();
			int feet = bi.readByte();
			int accessory1 = bi.readByte();
			int accessory2 = bi.readByte();
			int headColor = bi.readByte();
			int chestColor = bi.readByte();
			int handsColor = 0;
			bi.skipBytes(1);
			int waistColor = bi.readByte();
			int feetColor = bi.readByte();
			int accessory1Color = bi.readByte();
			int accessory2Color = bi.readByte();

			if (name.startsWith("@Chara") || name.startsWith("GM") || name.equalsIgnoreCase("SaveMGO")) {
				logger.error("createCharacter- Reserved prefix.");
				error = PacketError.CHAR_NAMEPREFIX;
				return;
			} else if (!Util.checkName(name)) {
				logger.error("createCharacter- Invalid name.");
				error = PacketError.CHAR_NAMEINVALID;
				return;
			}

			int takenId = handle.createQuery("""
					SELECT id
					FROM mgo2_charas
					WHERE name=:name
					""").bind("name", name).mapTo(Integer.class).findOne().orElse(0);
			if (takenId != 0) {
				logger.error("createCharacter- Name is taken.");
				error = PacketError.CHAR_NAMETAKEN;
				return;
			}

			long time = Instant.now().getEpochSecond();

			var chara = new Chara();
			chara.setUser(user.getId());
			chara.setName(name);
			chara.setCreationTime((int) time);
			chara.setGameplayOptions(CREATECHARACTER_GAMEPLAYOPTIONS);

			var appearance = new CharaAppearance();
			appearance.setGender(gender);
			appearance.setFace(face);
			appearance.setVoice(voice);
			appearance.setVoicePitch(voicePitch);
			appearance.setHead(head);
			appearance.setHeadColor(headColor);
			appearance.setUpper(upper);
			appearance.setUpperColor(upperColor);
			appearance.setLower(lower);
			appearance.setLowerColor(lowerColor);
			appearance.setChest(chest);
			appearance.setChestColor(chestColor);
			appearance.setWaist(waist);
			appearance.setWaistColor(waistColor);
			appearance.setHands(hands);
			appearance.setHandsColor(handsColor);
			appearance.setFeet(feet);
			appearance.setFeetColor(feetColor);
			appearance.setAccessory1(accessory1);
			appearance.setAccessory1Color(accessory1Color);
			appearance.setAccessory2(accessory2);
			appearance.setAccessory2Color(accessory2Color);
			appearance.setFacePaint(facePaint);

			var skills = new CharaSkills();

			boolean success = handle.inTransaction((h) -> {
				int id = h.createUpdate("""
						INSERT INTO mgo2_charas (user, name, creation_time, gameplay_options)
						VALUES (:user, :name, :creationTime, :gameplayOptions)
						""").bindBean(chara).executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne().orElse(0);
				if (id == 0) {
					logger.error("createCharacter- Failed to insert character.");
					h.rollback();
					return false;
				}
				chara.setId(id);

				appearance.setChara(id);
				int updated = h.createUpdate("""
					INSERT INTO mgo2_charas_appearance (chara, gender, face, voice, voice_pitch, head, head_color,
					upper, upper_color, lower, lower_color, chest, chest_color, waist, waist_color, hands,
					hands_color, feet, feet_color, accessory_1, accessory_1_color, accessory_2, accessory_2_color,
					face_paint)
					VALUES (:chara, :gender, :face, :voice, :voicePitch, :head, :headColor, :upper, :upperColor,
					:lower, :lowerColor, :chest, :chestColor, :waist, :waistColor, :hands, :handsColor, :feet,
					:feetColor, :accessory1, :accessory1Color, :accessory2, :accessory2Color, :facePaint)
					""").bindBean(appearance).execute();
				if (updated == 0) {
					logger.error("createCharacter- Failed to insert appearance.");
					h.rollback();
					return false;
				}

				skills.setChara(id);
				updated = h.createUpdate("""
						INSERT INTO mgo2_charas_skills (chara, skill_1, skill_1_level, skill_2, skill_2_level,
						skill_3, skill_3_level, skill_4, skill_4_level)
						VALUES (:chara, :skill1, :skill1Level, :skill2, :skill2Level, :skill3, :skill3Level,
						:skill4, :skill4Level)
						""").bindBean(skills).execute();
				if (updated == 0) {
					logger.error("createCharacter- Failed to insert skills.");
					h.rollback();
					return false;
				}

				return true;
			});

			if (!success) {
				error = PacketError.GENERAL;
				return;
			}

			bo = ctx.alloc().directBuffer(8);
			bo.writeInt(0).writeInt(chara.getId());

			ctx.write(new Packet(0x3102, bo));
		} catch (Exception e) {
			logger.error("createCharacter- Exception occurred.", e);
			error = PacketError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x3102, error);
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
			var localChara = new LocalChara();
			localUser.setCharacter(localChara);

			localChara.setUser(localUser);
			localChara.setLobby(localLobby);

			localChara.setId(chara.getId());
			localChara.setName(chara.getName());
			localChara.setNamePrefix(chara.getNamePrefix());
			localChara.setActive(chara.isActive());
		}

		LocalUsers.add(channel, localUser);
	}

	public static void onLobbyLeave(Channel channel) {
		LocalUsers.remove(channel);
	}

}
