package savemgo.nomad.helper;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.record.Chara;
import savemgo.nomad.database.record.CharaAppearance;
import savemgo.nomad.database.record.CharaBlocked;
import savemgo.nomad.database.record.CharaChatMacro;
import savemgo.nomad.database.record.CharaFriend;
import savemgo.nomad.database.record.CharaGearSet;
import savemgo.nomad.database.record.CharaSkillSet;
import savemgo.nomad.database.record.CharaSkills;
import savemgo.nomad.database.record.Clan;
import savemgo.nomad.local.util.LocalUsers;
import savemgo.nomad.packet.GameError;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Characters {

	private static final Logger logger = LogManager.getLogger();

	private static final byte CHARACTERINFO_BYTES1[] = { (byte) 0x16, (byte) 0xAE,
			//
			(byte) 0x03, (byte) 0x38,
			//
			(byte) 0x01, (byte) 0x3E,
			//
			(byte) 0x01, (byte) 0x50 };

	private static final byte CHARACTERINFO_BYTES2[] = { (byte) 0x00,
			//
			(byte) 0xB7, (byte) 0xFD, (byte) 0xAB, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF, (byte) 0x7B, (byte) 0x00,
			(byte) 0x00, (byte) 0x00,
			//
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			//
			(byte) 0x00,
			//
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };

	public static void getCharacterInfo(ChannelHandlerContext ctx, Handle handle) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getCharacterInfo- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get charactrer info
			var chara = handle.createQuery("SELECT name, name_prefix, exp " //
					+ "FROM mgo2_charas " //
					+ "WHERE id=:chara").bind("chara", localChara.getId()).mapToBean(Chara.class).one();

			// Get friends and block lists
			var friends = handle.createQuery("SELECT target " //
					+ "FROM mgo2_charas_friends " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaFriend.class).list();

			var block = handle.createQuery("SELECT target " //
					+ "FROM mgo2_charas_blocked " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaBlocked.class).list();

			long time = Instant.now().getEpochSecond();
			int secondLastLogin = (int) time - 2;
			int lastLogin = (int) time - 1;

			// Create payload
			bo = ctx.alloc().directBuffer(0x243);

			bo.writeInt(localChara.getId());
			Buffers.writeStringFill(bo, Util.getFullCharacterName(chara), 16);
			bo.writeBytes(CHARACTERINFO_BYTES1);

			bo.writeInt(chara.getExp()).writeInt(secondLastLogin).writeInt(lastLogin).writeZero(1);

			for (var friend : friends) {
				bo.writeInt(friend.getTarget());
			}
			bo.writeZero(0x129 - bo.writerIndex());

			for (var blocked : block) {
				bo.writeInt(blocked.getTarget());
			}
			bo.writeZero(0x229 - bo.writerIndex());

			bo.writeBytes(CHARACTERINFO_BYTES2);

			// Write payload
			ctx.write(new Packet(0x4101, bo));
		} catch (Exception e) {
			logger.error("getCharacterInfo- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4101, error);
		}
	}

	private static final byte GAMEPLAYOPTIONS_UI[] = { (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x11, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public static void getGameplayOptions(ChannelHandlerContext ctx, Handle handle) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getGameplayOptions- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get gameplay options
			var chara = handle.createQuery("SELECT gameplay_options " //
					+ "FROM mgo2_charas " //
					+ "WHERE id=:chara").bind("chara", localChara.getId()).mapToBean(Chara.class).one();

			// Parse json
			var data = Util.GSON.fromJson(chara.getGameplayOptions(), JsonObject.class);

			int onlineStatusMode = data.get("onlineStatusMode").getAsInt();
			boolean emailFriendsOnly = data.get("emailFriendsOnly").getAsBoolean();
			boolean receiveNotices = data.get("receiveNotices").getAsBoolean();
			boolean receiveInvites = data.get("receiveInvites").getAsBoolean();

			boolean normalViewVerticalInvert = data.get("normalViewVerticalInvert").getAsBoolean();
			boolean normalViewHorizontalInvert = data.get("normalViewHorizontalInvert").getAsBoolean();
			int normalViewSpeed = data.get("normalViewSpeed").getAsInt();
			boolean shoulderViewVerticalInvert = data.get("shoulderViewVerticalInvert").getAsBoolean();
			boolean shoulderViewHorizontalInvert = data.get("shoulderViewHorizontalInvert").getAsBoolean();
			int shoulderViewSpeed = data.get("shoulderViewSpeed").getAsInt();
			boolean firstViewVerticalInvert = data.get("firstViewVerticalInvert").getAsBoolean();
			boolean firstViewHorizontalInvert = data.get("firstViewHorizontalInvert").getAsBoolean();
			int firstViewSpeed = data.get("firstViewSpeed").getAsInt();

			boolean firstViewPlayerDirection = data.get("firstViewPlayerDirection").getAsBoolean();
			int viewChangeSpeed = data.get("viewChangeSpeed").getAsInt();
			boolean firstViewMemory = data.get("firstViewMemory").getAsBoolean();
			boolean radarLockNorth = data.get("radarLockNorth").getAsBoolean();
			boolean radarFloorHide = data.get("radarFloorHide").getAsBoolean();
			int hudDisplaySize = data.get("hudDisplaySize").getAsInt();
			boolean hudHideNameTags = data.get("hudHideNameTags").getAsBoolean();
			boolean lockOnEnabled = data.get("lockOnEnabled").getAsBoolean();

			int weaponSwitchMode = data.get("weaponSwitchMode").getAsInt();
			int weaponSwitchA = data.get("weaponSwitchA").getAsInt();
			int weaponSwitchB = data.get("weaponSwitchB").getAsInt();
			int weaponSwitchC = data.get("weaponSwitchC").getAsInt();

			int weaponSwitchNow = data.get("weaponSwitchNow").getAsInt();
			int weaponSwitchBefore = data.get("weaponSwitchBefore").getAsInt();

			int itemSwitchMode = data.get("itemSwitchMode").getAsInt();

			String codec1Name = data.get("codec1Name").getAsString();
			int codec1a = data.get("codec1a").getAsInt();
			int codec1b = data.get("codec1b").getAsInt();
			int codec1c = data.get("codec1c").getAsInt();
			int codec1d = data.get("codec1d").getAsInt();

			String codec2Name = data.get("codec2Name").getAsString();
			int codec2a = data.get("codec2a").getAsInt();
			int codec2b = data.get("codec2b").getAsInt();
			int codec2c = data.get("codec2c").getAsInt();
			int codec2d = data.get("codec2d").getAsInt();

			String codec3Name = data.get("codec3Name").getAsString();
			int codec3a = data.get("codec3a").getAsInt();
			int codec3b = data.get("codec3b").getAsInt();
			int codec3c = data.get("codec3c").getAsInt();
			int codec3d = data.get("codec3d").getAsInt();

			String codec4Name = data.get("codec4Name").getAsString();
			int codec4a = data.get("codec4a").getAsInt();
			int codec4b = data.get("codec4b").getAsInt();
			int codec4c = data.get("codec4c").getAsInt();
			int codec4d = data.get("codec4d").getAsInt();

			int voiceChatRecognitionLevel = data.get("voiceChatRecognitionLevel").getAsInt();
			int voiceChatVolume = data.get("voiceChatVolume").getAsInt();
			int headsetVolume = data.get("headsetVolume").getAsInt();
			int bgmVolume = data.get("bgmVolume").getAsInt();

			// Prepare vars
			viewChangeSpeed -= 1;

			int privacyA = 1;
			privacyA |= (onlineStatusMode & 0b11) << 4;
			privacyA |= emailFriendsOnly ? 0b01000000 : 0;

			int privacyB = 0;
			privacyB |= receiveNotices ? 0b1 : 0;
			privacyB |= receiveInvites ? 0b10000 : 0;

			int normalView = 0;
			normalView |= normalViewVerticalInvert ? 0b1 : 0;
			normalView |= normalViewHorizontalInvert ? 0b10 : 0;
			normalViewSpeed -= 1;
			normalView |= (normalViewSpeed & 0b1111) << 4;

			int shoulderView = 0;
			shoulderView |= shoulderViewVerticalInvert ? 0b1 : 0;
			shoulderView |= shoulderViewHorizontalInvert ? 0b10 : 0;
			shoulderViewSpeed -= 1;
			shoulderView |= (shoulderViewSpeed & 0b1111) << 4;

			int firstView = 0;
			firstView |= firstViewVerticalInvert ? 0b1 : 0;
			firstView |= firstViewHorizontalInvert ? 0b10 : 0;
			firstViewSpeed -= 1;
			firstView |= (firstViewSpeed & 0b1111) << 4;
			firstView |= firstViewPlayerDirection ? 0b100 : 0;

			byte _firstViewMemory = 0;
			_firstViewMemory |= firstViewMemory ? 0b10 : 0;

			int radar = 0;
			radar |= radarLockNorth ? 0b1 : 0;
			radar |= radarFloorHide ? 0b10000 : 0;

			int hudDisplay = 0;
			hudDisplay |= hudDisplaySize & 0b11;
			hudDisplay |= hudHideNameTags ? 0b10000 : 0;

			int lockOnAndBGM = 0;
			lockOnAndBGM |= lockOnEnabled ? 0b1 : 0;
			bgmVolume += 1;
			lockOnAndBGM |= (bgmVolume & 0b1111) << 4;

			int _weaponSwitchA = 0;
			_weaponSwitchA |= weaponSwitchA & 0b1111;
			_weaponSwitchA |= (weaponSwitchB & 0b1111) << 4;

			int _weaponSwitchB = 0;
			_weaponSwitchB |= weaponSwitchC & 0b1111;

			int weaponSwitchRecall = 0;
			weaponSwitchRecall |= weaponSwitchBefore & 0b1111;
			weaponSwitchRecall |= (weaponSwitchNow & 0b1111) << 4;

			int switchModes = 0;
			switchModes |= weaponSwitchMode & 0b1111;
			switchModes |= (itemSwitchMode & 0b1111) << 4;

			int voiceChatA = 1;
			voiceChatA |= (voiceChatRecognitionLevel & 0b1111) << 4;

			int voiceChatB = 0;
			voiceChatB |= voiceChatVolume & 0b1111;
			voiceChatB |= (headsetVolume & 0b1111) << 4;

			// Create payload
			bo = ctx.alloc().directBuffer(0x150);

			bo.writeByte(privacyA).writeByte(normalView).writeByte(shoulderView).writeByte(firstView)
					.writeByte(viewChangeSpeed).writeZero(6).writeByte(switchModes).writeZero(1).writeByte(voiceChatA)
					.writeByte(voiceChatB).writeByte(_weaponSwitchA).writeByte(_weaponSwitchB)
					.writeByte(weaponSwitchRecall).writeByte(_firstViewMemory).writeByte(privacyB)
					.writeByte(lockOnAndBGM).writeByte(radar).writeByte(hudDisplay).writeZero(9).writeByte(codec1a)
					.writeByte(codec1b).writeByte(codec1c).writeByte(codec1d).writeByte(codec2a).writeByte(codec2b)
					.writeByte(codec2c).writeByte(codec2d).writeByte(codec3a).writeByte(codec3b).writeByte(codec3c)
					.writeByte(codec3d).writeByte(codec4a).writeByte(codec4b).writeByte(codec4c).writeByte(codec4d);

			Buffers.writeStringFill(bo, codec1Name, 64);
			Buffers.writeStringFill(bo, codec2Name, 64);
			Buffers.writeStringFill(bo, codec3Name, 64);
			Buffers.writeStringFill(bo, codec4Name, 64);

			bo.writeBytes(GAMEPLAYOPTIONS_UI);

			// Write payload
			ctx.write(new Packet(0x4120, bo));
		} catch (Exception e) {
			logger.error("getGameplayOptions- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4120, error);
		}
	}

	public static void getChatMacros(ChannelHandlerContext ctx, Handle handle) {
		GameError error = null;
		PayloadGroup payloads = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getChatMacros- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get macros
			var macros = handle.createQuery("SELECT `index`, text " //
					+ "FROM mgo2_charas_chatmacros " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaChatMacro.class).list();

			String[] macrosText = new String[24];
			for (var macro : macros) {
				macrosText[macro.getIndex()] = macro.getText();
			}

			// Create payloads
			payloads = Buffers.createPayloads(2, 0x301, 1, (bo, i) -> {
				for (int j = 0; j < 12; j++) {
					String macro = macrosText[i * 12 + j];
					Buffers.writeStringFill(bo, macro, 64);
				}
			});

			// Write payload
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(0x4121, payload));
			}
		} catch (Exception e) {
			logger.error("getChatMacros- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(payloads);
		} finally {
			Packets.writeError(ctx, 0x4121, error);
		}
	}

	private static final byte PERSONALINFO_BYTES1[] = { (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C,
			(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

	private static final byte PERSONALINFO_BYTES3[] = { (byte) 0x00, (byte) 0xA7, (byte) 0x00, (byte) 0x0D };

	public static void getPersonalInfo(ChannelHandlerContext ctx, Handle handle) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getPersonalInfo- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get info
			handle.registerRowMapper(BeanMapper.factory(Chara.class, "c"));
			handle.registerRowMapper(BeanMapper.factory(CharaAppearance.class, "a"));
			handle.registerRowMapper(BeanMapper.factory(CharaSkills.class, "s"));
			handle.registerRowMapper(BeanMapper.factory(Clan.class, "k"));
			handle.registerRowMapper(
					JoinRowMapper.forTypes(Chara.class, CharaAppearance.class, CharaSkills.class, Clan.class));

			var row = handle.createQuery("SELECT c.comment c_comment, c.rank c_rank, c.exp c_exp, "
					+ "a.gender a_gender, a.face a_face, a.voice a_voice, a.voice_pitch a_voice_pitch, "
					+ "a.head a_head, a.head_color a_head_color, a.upper a_upper, a.upper_color a_upper_color, "
					+ "a.lower a_lower, a.lower_color a_lower_color, a.chest a_chest, a.chest_color a_chest_color, "
					+ "a.waist a_waist, a.waist_color a_waist_color, a.hands a_hands, a.hands_color a_hands_color, "
					+ "a.feet a_feet, a.feet_color a_feet_color, a.accessory_1 a_accessory_1, "
					+ "a.accessory_1_color a_accessory_1_color, a.accessory_2 a_accessory_2, "
					+ "a.accessory_2_color a_accessory_2_color, a.face_paint a_face_paint, "
					+ "s.skill_1 s_skill_1, s.skill_1_level s_skill_1_level, s.skill_2 s_skill_2, "
					+ "s.skill_2_level s_skill_2_level, s.skill_3 s_skill_3, s.skill_3_level s_skill_3_level, "
					+ "s.skill_4 s_skill_4, s.skill_4_level s_skill_4_level, " //
					+ "k.id k_id, k.name k_name, k.emblem k_emblem " //
					+ "FROM mgo2_charas c " //
					+ "JOIN mgo2_charas_appearance a ON a.chara=c.id " //
					+ "JOIN mgo2_charas_skills s ON s.chara=c.id " //
					+ "LEFT JOIN mgo2_clans_members m ON m.chara=c.id " //
					+ "LEFT JOIN mgo2_clans k ON m.clan=k.id " //
					+ "WHERE c.id=:chara").bind("chara", localChara.getId()).mapTo(JoinRow.class).one();

			var chara = row.get(Chara.class);
			var appearance = row.get(CharaAppearance.class);
			var skills = row.get(CharaSkills.class);
			var clan = row.get(Clan.class);

			long time = Instant.now().getEpochSecond();
			int rwd = chara.getExp();

			// Create payload
			bo = ctx.alloc().directBuffer(0xf5);

			bo.writeInt(clan.getId());
			Buffers.writeStringFill(bo, clan.getName(), 16);

			bo.writeBytes(PERSONALINFO_BYTES1).writeInt((int) time);

			bo.writeByte(appearance.getGender()).writeByte(appearance.getFace()).writeByte(appearance.getUpper())
					.writeByte(appearance.getLower()).writeByte(appearance.getFacePaint())
					.writeByte(appearance.getUpperColor()).writeByte(appearance.getLowerColor())
					.writeByte(appearance.getVoice()).writeByte(appearance.getVoicePitch()).writeZero(4)
					.writeByte(appearance.getHead()).writeByte(appearance.getChest()).writeByte(appearance.getHands())
					.writeByte(appearance.getWaist()).writeByte(appearance.getFeet())
					.writeByte(appearance.getAccessory1()).writeByte(appearance.getAccessory2())
					.writeByte(appearance.getHeadColor()).writeByte(appearance.getChestColor())
					.writeByte(appearance.getHandsColor()).writeByte(appearance.getWaistColor())
					.writeByte(appearance.getFeetColor()).writeByte(appearance.getAccessory1Color())
					.writeByte(appearance.getAccessory2Color());

			bo.writeByte(skills.getSkill1()).writeByte(skills.getSkill2()).writeByte(skills.getSkill3())
					.writeByte(skills.getSkill4()).writeZero(1).writeByte(skills.getSkill1Level())
					.writeByte(skills.getSkill2Level()).writeByte(skills.getSkill3Level())
					.writeByte(skills.getSkill4Level()).writeZero(1);

			int skillExp = 0x600000;
			bo.writeInt(skillExp).writeInt(skillExp).writeInt(skillExp).writeInt(skillExp).writeZero(5);

			bo.writeInt(rwd);

			if (chara.getComment() != null) {
				Buffers.writeStringFill(bo, chara.getComment(), 128);
			} else {
				bo.writeZero(128);
			}

			bo.writeByte(chara.getRank());

			if (clan.getId() != 0 && clan.getEmblem() != null) {
				bo.writeByte(3);
			} else {
				bo.writeByte(0);
			}

			bo.writeBytes(PERSONALINFO_BYTES3);

			// Write payload
			ctx.write(new Packet(0x4122, bo));
		} catch (Exception e) {
			logger.error("getPersonalInfo- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4122, error);
		}
	}

	public static void setPersonalInfo(ChannelHandlerContext ctx, Packet in) {
		GameError error = null;
		ByteBuf bo = null;
		try (var handle = DB.open()) {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("setPersonalInfo- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get info
			handle.registerRowMapper(BeanMapper.factory(Chara.class, "c"));
			handle.registerRowMapper(BeanMapper.factory(CharaAppearance.class, "a"));
			handle.registerRowMapper(BeanMapper.factory(CharaSkills.class, "s"));
			handle.registerRowMapper(BeanMapper.factory(Clan.class, "k"));
			handle.registerRowMapper(
					JoinRowMapper.forTypes(Chara.class, CharaAppearance.class, CharaSkills.class, Clan.class));

			var row = handle.createQuery("SELECT c.comment c_comment, c.rank c_rank, c.exp c_exp, "
					+ "a.gender a_gender, a.face a_face, a.voice a_voice, a.voice_pitch a_voice_pitch, "
					+ "a.head a_head, a.head_color a_head_color, a.upper a_upper, a.upper_color a_upper_color, "
					+ "a.lower a_lower, a.lower_color a_lower_color, a.chest a_chest, a.chest_color a_chest_color, "
					+ "a.waist a_waist, a.waist_color a_waist_color, a.hands a_hands, a.hands_color a_hands_color, "
					+ "a.feet a_feet, a.feet_color a_feet_color, a.accessory_1 a_accessory_1, "
					+ "a.accessory_1_color a_accessory_1_color, a.accessory_2 a_accessory_2, "
					+ "a.accessory_2_color a_accessory_2_color, a.face_paint a_face_paint, "
					+ "s.skill_1 s_skill_1, s.skill_1_level s_skill_1_level, s.skill_2 s_skill_2, "
					+ "s.skill_2_level s_skill_2_level, s.skill_3 s_skill_3, s.skill_3_level s_skill_3_level, "
					+ "s.skill_4 s_skill_4, s.skill_4_level s_skill_4_level, " //
					+ "k.id k_id, k.name k_name, k.emblem k_emblem " //
					+ "FROM mgo2_charas c " //
					+ "JOIN mgo2_charas_appearance a ON a.chara=c.id " //
					+ "JOIN mgo2_charas_skills s ON s.chara=c.id " //
					+ "LEFT JOIN mgo2_clans_members m ON m.chara=c.id " //
					+ "LEFT JOIN mgo2_clans k ON m.clan=k.id " //
					+ "WHERE c.id=:chara").bind("chara", localChara.getId()).mapTo(JoinRow.class).one();

			var chara = row.get(Chara.class);
			var appearance = row.get(CharaAppearance.class);
			var skills = row.get(CharaSkills.class);

			// Create payload
			bo = ctx.alloc().directBuffer(0xba);

			bo.writeInt(0);

			bo.writeByte(appearance.getUpper()).writeByte(appearance.getLower()).writeByte(appearance.getFacePaint())
					.writeByte(appearance.getUpperColor()).writeByte(appearance.getLowerColor())
					.writeByte(appearance.getHead()).writeByte(appearance.getChest()).writeByte(appearance.getHands())
					.writeByte(appearance.getWaist()).writeByte(appearance.getFeet())
					.writeByte(appearance.getAccessory1()).writeByte(appearance.getAccessory2())
					.writeByte(appearance.getHeadColor()).writeByte(appearance.getChestColor())
					.writeByte(appearance.getHandsColor()).writeByte(appearance.getWaistColor())
					.writeByte(appearance.getFeetColor()).writeByte(appearance.getAccessory1Color())
					.writeByte(appearance.getAccessory2Color());

			bo.writeByte(skills.getSkill1()).writeByte(skills.getSkill2()).writeByte(skills.getSkill3())
					.writeByte(skills.getSkill4()).writeZero(1).writeByte(skills.getSkill1Level())
					.writeByte(skills.getSkill2Level()).writeByte(skills.getSkill3Level())
					.writeByte(skills.getSkill4Level()).writeZero(1);

			int skillExp = 0x600000;
			bo.writeInt(skillExp).writeInt(skillExp).writeInt(skillExp).writeInt(skillExp).writeZero(5);

			Buffers.writeStringFill(bo, chara.getComment(), 128);

			// Write payload
			ctx.write(new Packet(0x4131, bo));
		} catch (Exception e) {
			logger.error("setPersonalInfo- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4131, error);
		}
	}

	private static final int[] AVAILABLEGEAR_ITEMS = { 0x04, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x16,
			0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2E, 0x2F,
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40, 0x44, 0x45, 0x46, 0x47,
			0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B,
			0x5C, 0x5D, 0x5E, 0x5F, 0x60, 0x61, 0x62, 0x63, 0x64, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E,
			0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x86, 0x87,
			0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F, 0xA0, 0xA1, 0xA2, 0xB0, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xF0,
			0xF1, 0xF2, 0xF3, 0xF4 };

	private static final byte[] AVAILABLEGEAR_END = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff };

	public static void getAvailableGear(ChannelHandlerContext ctx) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getAvailableGear- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			// Create payload
			bo = ctx.alloc().directBuffer(AVAILABLEGEAR_ITEMS.length * 5 + AVAILABLEGEAR_END.length);

			bo.writeInt(AVAILABLEGEAR_ITEMS.length);

			for (int gearItem : AVAILABLEGEAR_ITEMS) {
				int colors = 0xffffffff;
				bo.writeByte(gearItem).writeInt(colors);
			}

			bo.writeBytes(AVAILABLEGEAR_END);

			// Write payload
			ctx.write(new Packet(0x4124, bo));
		} catch (Exception e) {
			logger.error("getAvailableGear- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4124, error);
		}
	}

	public static void getAvailableSkills(ChannelHandlerContext ctx) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getAvailableSkills- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			// Create payload
			int numSkills = 25;

			bo = ctx.alloc().directBuffer(4 + numSkills * 4);

			bo.writeInt(numSkills);

			for (int skill = 1; skill < numSkills; skill++) {
				int exp = (skill == 17 || skill == 20 || skill == 22) ? 0x2000 : 0x6000;
				bo.writeByte(skill).writeShort(exp).writeZero(1);
			}

			// Write payload
			ctx.write(new Packet(0x4125, bo));
		} catch (Exception e) {
			logger.error("getAvailableSkills- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4125, error);
		}
	}

	public static void getSkillSets(ChannelHandlerContext ctx, Handle handle) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getSkillSets- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get skill sets
			var setList = handle.createQuery("SELECT `index`, name, modes, skill_1, skill_1_level " //
					+ "skill_2, skill_2_level, skill_3, skill_3_level, skill_4, skill_4_level " //
					+ "FROM mgo2_charas_skillsets " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaSkillSet.class).list();

			CharaSkillSet[] sets = new CharaSkillSet[3];
			for (var set : setList) {
				sets[set.getIndex()] = set;
			}

			// Create payload
			bo = ctx.alloc().directBuffer(sets.length * 77);

			for (var set : sets) {
				if (set != null) {
					bo.writeInt(set.getModes()).writeByte(set.getSkill1()).writeByte(set.getSkill2())
							.writeByte(set.getSkill3()).writeByte(set.getSkill4()).writeZero(1)
							.writeByte(set.getSkill1Level()).writeByte(set.getSkill2Level())
							.writeByte(set.getSkill3Level()).writeByte(set.getSkill4Level()).writeZero(1);
					Buffers.writeStringFill(bo, set.getName(), 63);
				} else {
					bo.writeZero(77);
				}
			}

			// Write payload
			ctx.write(new Packet(0x4140, bo));
		} catch (Exception e) {
			logger.error("getSkillSets- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4140, error);
		}
	}

	public static void getGearSets(ChannelHandlerContext ctx, Handle handle) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getSkillSets- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			var localChara = user.getCharacter();

			// Get skill sets
			var setList = handle.createQuery("SELECT `index`, name, stages, face, head, head_color, upper, " //
					+ "upper_color, lower, lower_color, chest, chest_color, waist, waist_color, hands, " //
					+ "hands_color, feet, feet_color, accessory_1, accessory_1_color, accessory_2, " //
					+ "accessory_2_color, face_paint " //
					+ "FROM mgo2_charas_gearsets " //
					+ "WHERE chara=:chara").bind("chara", localChara.getId()).mapToBean(CharaGearSet.class).list();

			CharaGearSet[] sets = new CharaGearSet[3];
			for (var set : setList) {
				sets[set.getIndex()] = set;
			}

			// Create payload
			bo = ctx.alloc().directBuffer(sets.length * 87);

			for (var set : sets) {
				if (set != null) {
					bo.writeInt(set.getStages()).writeByte(set.getFace()).writeByte(set.getHead())
							.writeByte(set.getUpper()).writeByte(set.getLower()).writeByte(set.getChest())
							.writeByte(set.getWaist()).writeByte(set.getHands()).writeByte(set.getFeet())
							.writeByte(set.getAccessory1()).writeByte(set.getAccessory2()).writeByte(set.getHeadColor())
							.writeByte(set.getUpperColor()).writeByte(set.getLowerColor())
							.writeByte(set.getChestColor()).writeByte(set.getWaistColor())
							.writeByte(set.getHandsColor()).writeByte(set.getFeetColor())
							.writeByte(set.getAccessory1Color()).writeByte(set.getAccessory2Color())
							.writeByte(set.getFacePaint());
					Buffers.writeStringFill(bo, set.getName(), 63);
				} else {
					bo.writeZero(87);
				}
			}

			// Write payload
			ctx.write(new Packet(0x4142, bo));
		} catch (Exception e) {
			logger.error("getGearSets- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4142, error);
		}
	}

	public static void onLobbyDisconnect(ChannelHandlerContext ctx, Packet in) {
		// In: 00
		ctx.write(new Packet(0x4151, 0));
	}

	public static void getGameEntryInfo(ChannelHandlerContext ctx) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getGameEntryInfo- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			// Create payload
			bo = ctx.alloc().directBuffer(0xac);

			bo.writeInt(0).writeInt(1).writeZero(0xa4);

			// Write payload
			ctx.write(new Packet(0x4991, bo));
		} catch (Exception e) {
			logger.error("getGameEntryInfo- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4991, error);
		}
	}

}
