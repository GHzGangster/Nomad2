package savemgo.nomad.helper;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.local.util.LocalGames;
import savemgo.nomad.local.util.LocalUsers;
import savemgo.nomad.packet.GameError;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.PayloadGroup;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Games {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet GETLIST_START_4301 = new Packet(0x4301, 0);
	private static final Packet GETLIST_END_4301 = new Packet(0x4303, 0);

	public static void getList(ChannelHandlerContext ctx, Packet in, LocalLobby lobby, int commandStart) {
		GameError error = null;
		PayloadGroup payloads = null;
		try {
			// Set up command ids and packets
			int commandData = 0;
			Packet packetStart = null;
			Packet packetEnd = null;

			switch (commandStart) {
			case 0x4301:
				commandData = 0x4302;
				packetStart = GETLIST_START_4301;
				packetEnd = GETLIST_END_4301;
				break;
			default:
				throw new IllegalArgumentException("Unknown command id.");
			}

			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getList- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			// Read payload
			ByteBuf bi = in.getPayload();

			/**
			 * 00 00 02 00 - Clan Room
			 * 
			 * 00 00 00 02 - Free Battle
			 */
			int type = bi.readInt();

			// Get games in this lobby
			var games = LocalGames.get().stream().filter((e) -> e.getLobby() == lobby).collect(Collectors.toList());

			// Create payloads
			payloads = Buffers.createPayloads(games.size(), 0x37, 18, (bo, i) -> {
				var game = games.get(i);

				// Get common rules
				var common = Util.GSON.fromJson(game.getCommon(), JsonObject.class);
				boolean dedicated = common.get("dedicated").getAsBoolean();
				boolean friendlyFire = common.get("friendlyFire").getAsBoolean();
				boolean autoAim = common.get("autoAim").getAsBoolean();

				var uniques = common.get("uniques").getAsJsonObject();
				boolean uniquesEnabled = uniques.get("enabled").getAsBoolean();

				boolean enemyNametags = common.get("enemyNametags").getAsBoolean();
				boolean silentMode = common.get("silentMode").getAsBoolean();
				boolean autoAssign = common.get("autoAssign").getAsBoolean();
				boolean teamsSwitch = common.get("teamsSwitch").getAsBoolean();
				boolean ghosts = common.get("ghosts").getAsBoolean();

				var levelLimit = common.get("levelLimit").getAsJsonObject();
				boolean levelLimitEnabled = levelLimit.get("enabled").getAsBoolean();
				levelLimitEnabled = false;
				int levelLimitBase = levelLimit.get("base").getAsInt();
				int levelLimitTolerance = levelLimit.get("tolerance").getAsInt();

				boolean voiceChat = common.get("voiceChat").getAsBoolean();
				int teamKillKick = common.get("teamKillKick").getAsInt();
				int idleKick = common.get("idleKick").getAsInt();

				// Get current rule and map
				var gameSet = Util.GSON.fromJson(game.getGames(), JsonArray.class);

				int currentGame = game.getCurrentGame();

				int rule = 0, map = 0;
				if (currentGame < gameSet.size()) {
					var g = gameSet.get(currentGame).getAsJsonArray();
					rule = g.get(0).getAsInt();
					map = g.get(1).getAsInt();
				} else {
					rule = 0;
					map = 0;
				}

				// Get player count, average experience, and if a friend/blocked player is
				// present
				var players = game.getPlayers();
				var playersLock = game.getPlayersLock();

				int numPlayers = 0;
				int averageExperience = 0;
				boolean hasFriend = false, hasBlocked = false;

				try {
					playersLock.readLock().lock();

					numPlayers = players.size();

					for (var player : players) {
						var chara = player.getChara();
						int exp = 0;
						averageExperience += exp;

//						if (!hasFriend && character.getFriends().stream()
//								.anyMatch(e -> e.getTargetId().equals(player.getCharacterId()))) {
//							hasFriend = true;
//						}
//						if (!hasBlocked && character.getBlocked().stream()
//								.anyMatch(e -> e.getTargetId().equals(player.getCharacterId()))) {
//							hasBlocked = true;
//						}
					}
				} finally {
					playersLock.readLock().unlock();
				}

				if (numPlayers > 0) {
					averageExperience /= numPlayers;
				}

				// Placeholders
				int hostScore = 0;
				int hostVotes = 0;

				// Set up flags
				int hostOptions = 0;
				hostOptions |= game.getPassword() != null ? 0b1 : 0;
				hostOptions |= dedicated ? 0b10 : 0;

				int commonA = 0b100;
				commonA |= idleKick > 0 ? 0b1 : 0;
				commonA |= friendlyFire ? 0b1000 : 0;
				commonA |= ghosts ? 0b10000 : 0;
				commonA |= autoAim ? 0b100000 : 0;
				commonA |= uniquesEnabled ? 0b10000000 : 0;

				int commonB = 0;
				commonB |= teamsSwitch ? 0b1 : 0;
				commonB |= autoAssign ? 0b10 : 0;
				commonB |= silentMode ? 0b100 : 0;
				commonB |= enemyNametags ? 0b1000 : 0;
				commonB |= levelLimitEnabled ? 0b10000 : 0;
				commonB |= voiceChat ? 0b1000000 : 0;
				commonB |= teamKillKick > 0 ? 0b10000000 : 0;

				int friendBlock = 0;
				friendBlock |= hasFriend ? 0b1 : 0;
				friendBlock |= hasBlocked ? 0b10 : 0;

				int unknown = 0x8;

				bo.writeInt(game.getId());
				Buffers.writeStringFill(bo, game.getName(), 16);
				bo.writeByte(hostOptions).writeByte(unknown).writeByte(rule).writeByte(map).writeZero(1)
						.writeByte(game.getMaxPlayers()).writeByte(game.getStance()).writeByte(commonA)
						.writeByte(commonB).writeByte(numPlayers).writeInt(game.getPing()).writeByte(friendBlock)
						.writeByte(levelLimitTolerance).writeInt(levelLimitBase).writeInt(averageExperience)
						.writeInt(hostScore).writeInt(hostVotes).writeZero(2).writeByte(0x63);
			});

			// Write payloads
			ctx.write(packetStart);
			for (var payload : payloads.getBuffers()) {
				ctx.write(new Packet(commandData, payload));
			}
			ctx.write(packetEnd);
		} catch (Exception e) {
			logger.error("getList- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(payloads);
		} finally {
			Packets.writeError(ctx, commandStart, error);
		}
	}

	public static void getDetails(ChannelHandlerContext ctx, Packet in, LocalLobby lobby) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("getDetails- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}

			// Read payload
			ByteBuf bi = in.getPayload();

			int gameId = bi.readInt();

			// Get game
			var game = LocalGames.get(gameId);

			// Create payload
			bo = ctx.alloc().directBuffer(0x36d);

			// Get common rules
			var common = Util.GSON.fromJson(game.getCommon(), JsonObject.class);
			int briefingTime = common.get("briefingTime").getAsInt();
			boolean nonStat = common.get("nonStat").getAsBoolean();
			boolean friendlyFire = common.get("friendlyFire").getAsBoolean();
			boolean autoAim = common.get("autoAim").getAsBoolean();

			var uniques = common.get("uniques").getAsJsonObject();
			boolean uniquesEnabled = uniques.get("enabled").getAsBoolean();
			boolean uniquesRandom = uniques.get("random").getAsBoolean();
			int uniqueRed = uniques.get("red").getAsInt();
			int uniqueBlue = uniques.get("blue").getAsInt();

			boolean enemyNametags = common.get("enemyNametags").getAsBoolean();
			boolean silentMode = common.get("silentMode").getAsBoolean();
			boolean autoAssign = common.get("autoAssign").getAsBoolean();
			boolean teamsSwitch = common.get("teamsSwitch").getAsBoolean();
			boolean ghosts = common.get("ghosts").getAsBoolean();

			var levelLimit = common.get("levelLimit").getAsJsonObject();
			boolean levelLimitEnabled = levelLimit.get("enabled").getAsBoolean();
			levelLimitEnabled = false;
			int levelLimitBase = levelLimit.get("base").getAsInt();
			int levelLimitTolerance = levelLimit.get("tolerance").getAsInt();

			boolean voiceChat = common.get("voiceChat").getAsBoolean();
			int teamKillKick = common.get("teamKillKick").getAsInt();
			int idleKick = common.get("idleKick").getAsInt();

			// Get weapon restrictions
			var weaponRestrictions = common.get("weaponRestrictions").getAsJsonObject();
			boolean weaponRestrictionEnabled = weaponRestrictions.get("enabled").getAsBoolean();

			var wrPrimary = weaponRestrictions.get("primary").getAsJsonObject();
			boolean vz = wrPrimary.get("vz").getAsBoolean();
			boolean p90 = wrPrimary.get("p90").getAsBoolean();
			boolean mp5 = wrPrimary.get("mp5").getAsBoolean();
			boolean patriot = wrPrimary.get("patriot").getAsBoolean();
			boolean ak = wrPrimary.get("ak").getAsBoolean();
			boolean m4 = wrPrimary.get("m4").getAsBoolean();
			boolean mk17 = wrPrimary.get("mk17").getAsBoolean();
			boolean xm8 = wrPrimary.get("xm8").getAsBoolean();
			boolean g3a3 = wrPrimary.get("g3a3").getAsBoolean();
			boolean svd = wrPrimary.get("svd").getAsBoolean();
			boolean mosin = wrPrimary.get("mosin").getAsBoolean();
			boolean m14 = wrPrimary.get("m14").getAsBoolean();
			boolean vss = wrPrimary.get("vss").getAsBoolean();
			boolean dsr = wrPrimary.get("dsr").getAsBoolean();
			boolean m870 = wrPrimary.get("m870").getAsBoolean();
			boolean saiga = wrPrimary.get("saiga").getAsBoolean();
			boolean m60 = wrPrimary.get("m60").getAsBoolean();
			boolean shield = wrPrimary.get("shield").getAsBoolean();
			boolean rpg = wrPrimary.get("rpg").getAsBoolean();
			boolean knife = wrPrimary.get("knife").getAsBoolean();

			var wrSecondary = weaponRestrictions.get("secondary").getAsJsonObject();
			boolean gsr = wrSecondary.get("gsr").getAsBoolean();
			boolean mk2 = wrSecondary.get("mk2").getAsBoolean();
			boolean operator = wrSecondary.get("operator").getAsBoolean();
			boolean g18 = wrSecondary.get("g18").getAsBoolean();
			boolean mk23 = wrSecondary.get("mk23").getAsBoolean();
			boolean de = wrSecondary.get("de").getAsBoolean();

			var wrSupport = weaponRestrictions.get("support").getAsJsonObject();
			boolean grenade = wrSupport.get("grenade").getAsBoolean();
			boolean wp = wrSupport.get("wp").getAsBoolean();
			boolean stun = wrSupport.get("stun").getAsBoolean();
			boolean chaff = wrSupport.get("chaff").getAsBoolean();
			boolean smoke = wrSupport.get("smoke").getAsBoolean();
			boolean smoke_r = wrSupport.get("smoke_r").getAsBoolean();
			boolean smoke_g = wrSupport.get("smoke_g").getAsBoolean();
			boolean smoke_y = wrSupport.get("smoke_y").getAsBoolean();
			boolean eloc = wrSupport.get("eloc").getAsBoolean();
			boolean claymore = wrSupport.get("claymore").getAsBoolean();
			boolean sgmine = wrSupport.get("sgmine").getAsBoolean();
			boolean c4 = wrSupport.get("c4").getAsBoolean();
			boolean sgsatchel = wrSupport.get("sgsatchel").getAsBoolean();
			boolean magazine = wrSupport.get("magazine").getAsBoolean();

			var wrCustom = weaponRestrictions.get("custom").getAsJsonObject();
			boolean suppressor = wrCustom.get("suppressor").getAsBoolean();
			boolean gp30 = wrCustom.get("gp30").getAsBoolean();
			boolean xm320 = wrCustom.get("xm320").getAsBoolean();
			boolean masterkey = wrCustom.get("masterkey").getAsBoolean();
			boolean scope = wrCustom.get("scope").getAsBoolean();
			boolean sight = wrCustom.get("sight").getAsBoolean();
			boolean laser = wrCustom.get("laser").getAsBoolean();
			boolean lighthg = wrCustom.get("lighthg").getAsBoolean();
			boolean lightlg = wrCustom.get("lightlg").getAsBoolean();
			boolean grip = wrCustom.get("grip").getAsBoolean();

			var wrItems = weaponRestrictions.get("items").getAsJsonObject();
			boolean envg = wrItems.get("envg").getAsBoolean();
			boolean drum = wrItems.get("drum").getAsBoolean();

			// Get mode rules
			var ruleSettings = Util.GSON.fromJson(game.getRules(), JsonObject.class);

			var dm = ruleSettings.get("dm").getAsJsonObject();
			int dmTime = dm.get("time").getAsInt();
			int dmRounds = dm.get("rounds").getAsInt();
			int dmTickets = dm.get("tickets").getAsInt();

			var tdm = ruleSettings.get("tdm").getAsJsonObject();
			int tdmTime = tdm.get("time").getAsInt();
			int tdmRounds = tdm.get("rounds").getAsInt();
			int tdmTickets = tdm.get("tickets").getAsInt();

			var res = ruleSettings.get("res").getAsJsonObject();
			int resTime = res.get("time").getAsInt();
			int resRounds = res.get("rounds").getAsInt();

			var cap = ruleSettings.get("cap").getAsJsonObject();
			int capTime = cap.get("time").getAsInt();
			int capRounds = cap.get("rounds").getAsInt();
			boolean capExtraTime = cap.get("extraTime").getAsBoolean();

			var sne = ruleSettings.get("sne").getAsJsonObject();
			int sneTime = sne.get("time").getAsInt();
			int sneRounds = sne.get("rounds").getAsInt();
			int sneSnake = sne.get("snake").getAsInt();

			var base = ruleSettings.get("base").getAsJsonObject();
			int baseTime = base.get("time").getAsInt();
			int baseRounds = base.get("rounds").getAsInt();

			var bomb = ruleSettings.get("bomb").getAsJsonObject();
			int bombTime = bomb.get("time").getAsInt();
			int bombRounds = bomb.get("rounds").getAsInt();

			var tsne = ruleSettings.get("tsne").getAsJsonObject();
			int tsneTime = tsne.get("time").getAsInt();
			int tsneRounds = tsne.get("rounds").getAsInt();

			var sdm = ruleSettings.get("sdm").getAsJsonObject();
			int sdmTime = sdm.get("time").getAsInt();
			int sdmRounds = sdm.get("rounds").getAsInt();

			var intr = ruleSettings.get("int").getAsJsonObject();
			int intTime = intr.get("time").getAsInt();

			var scap = ruleSettings.get("scap").getAsJsonObject();
			int scapTime = scap.get("time").getAsInt();
			int scapRounds = scap.get("rounds").getAsInt();
			boolean scapExtraTime = scap.get("extraTime").getAsBoolean();

			var race = ruleSettings.get("race").getAsJsonObject();
			int raceTime = race.get("time").getAsInt();
			int raceRounds = race.get("rounds").getAsInt();
			boolean raceExtraTime = race.get("extraTime").getAsBoolean();

			// Get rules and maps
			var gameSet = Util.GSON.fromJson(game.getGames(), JsonArray.class);

			// Get player count, average experience, and if a friend/blocked player is
			// present
			var players = game.getPlayers();
			var playersLock = game.getPlayersLock();

			int numPlayers = 0;
			int averageExperience = 0;

			try {
				playersLock.readLock().lock();

				numPlayers = players.size();

				for (var player : players) {
					var chara = player.getChara();
					int exp = 0;
					averageExperience += exp;
				}
			} finally {
				playersLock.readLock().unlock();
			}

			if (numPlayers > 0) {
				averageExperience /= numPlayers;
			}

			// Placeholders
			int hostScore = 0;
			int hostVotes = 0;

			// Set up flags
			int commonA = 0b100;
			commonA |= idleKick > 0 ? 0b1 : 0;
			commonA |= friendlyFire ? 0b1000 : 0;
			commonA |= ghosts ? 0b10000 : 0;
			commonA |= autoAim ? 0b100000 : 0;
			commonA |= uniquesEnabled ? 0b10000000 : 0;

			int commonB = 0;
			commonB |= teamsSwitch ? 0b1 : 0;
			commonB |= autoAssign ? 0b10 : 0;
			commonB |= silentMode ? 0b100 : 0;
			commonB |= enemyNametags ? 0b1000 : 0;
			commonB |= levelLimitEnabled ? 0b10000 : 0;
			commonB |= voiceChat ? 0b1000000 : 0;
			commonB |= teamKillKick > 0 ? 0b10000000 : 0;

			int hostOptionsExtraTimeFlags = 0;
			hostOptionsExtraTimeFlags |= !scapExtraTime ? 0b1 : 0;
			hostOptionsExtraTimeFlags |= nonStat ? 0b10 : 0;
			hostOptionsExtraTimeFlags |= !raceExtraTime ? 0b100 : 0;

			byte[] wr = new byte[0x10];
			wr[0] |= weaponRestrictionEnabled ? 0b1 : 0;
			wr[0] |= !knife ? 0b10 : 0;
			wr[0] |= !mk2 ? 0b100 : 0;
			wr[0] |= !operator ? 0b1000 : 0;
			wr[0] |= !mk23 ? 0b10000 : 0;
			wr[0] |= !gsr ? 0b10000000 : 0;

			wr[1] |= !de ? 0b1 : 0;
			wr[1] |= !g18 ? 0b10000000 : 0;

			wr[2] |= !mp5 ? 0b100 : 0;
			wr[2] |= !p90 ? 0b10000 : 0;
			wr[2] |= !patriot ? 0b1000000 : 0;
			wr[2] |= !vz ? 0b10000000 : 0;

			wr[3] |= !m4 ? 0b1 : 0;
			wr[3] |= !ak ? 0b10 : 0;
			wr[3] |= !g3a3 ? 0b100 : 0;
			wr[3] |= !mk17 ? 0b1000000 : 0;
			wr[3] |= !xm8 ? 0b10000000 : 0;

			wr[4] |= !m60 ? 0b1000 : 0;
			wr[4] |= !m870 ? 0b100000 : 0;
			wr[4] |= !saiga ? 0b1000000 : 0;
			wr[4] |= !vss ? 0b10000000 : 0;

			wr[5] |= !dsr ? 0b10 : 0;
			wr[5] |= !m14 ? 0b100 : 0;
			wr[5] |= !mosin ? 0b1000 : 0;
			wr[5] |= !svd ? 0b10000 : 0;

			wr[6] |= !rpg ? 0b100 : 0;
			wr[6] |= !grenade ? 0b10000 : 0;
			wr[6] |= !wp ? 0b100000 : 0;
			wr[6] |= !stun ? 0b1000000 : 0;
			wr[6] |= !chaff ? 0b10000000 : 0;

			wr[7] |= !smoke ? 0b1 : 0;
			wr[7] |= !smoke_r ? 0b10 : 0;
			wr[7] |= !smoke_g ? 0b100 : 0;
			wr[7] |= !smoke_y ? 0b1000 : 0;
			wr[7] |= !eloc ? 0b10000000 : 0;

			wr[8] |= !claymore ? 0b1 : 0;
			wr[8] |= !sgmine ? 0b10 : 0;
			wr[8] |= !c4 ? 0b100 : 0;
			wr[8] |= !sgsatchel ? 0b1000 : 0;
			wr[8] |= !magazine ? 0b100000 : 0;

			wr[9] |= !shield ? 0b10 : 0;
			wr[9] |= !masterkey ? 0b100 : 0;
			wr[9] |= !xm320 ? 0b1000 : 0;
			wr[9] |= !gp30 ? 0b10000 : 0;
			wr[9] |= !suppressor ? 0b100000 : 0;

			wr[10] |= !suppressor ? 0b1110 : 0;

			wr[11] |= !scope ? 0b10000 : 0;
			wr[11] |= !sight ? 0b100000 : 0;
			wr[11] |= !lightlg ? 0b10000000 : 0;

			wr[12] |= !laser ? 0b1 : 0;
			wr[12] |= !lighthg ? 0b10 : 0;
			wr[12] |= !grip ? 0b100 : 0;

			wr[13] |= !drum ? 0b100 : 0;

			wr[14] |= !envg ? 0b1000000 : 0;

			bo.writeInt(0).writeInt(game.getId());
			Buffers.writeStringFill(bo, game.getName(), 16);
			Buffers.writeStringFill(bo, game.getComment(), 128);
			bo.writeZero(2).writeByte(lobby.getSubtype()).writeInt(averageExperience).writeInt(hostScore)
					.writeInt(hostVotes).writeByte(0x1);

			// Write rules, maps, and flags
			for (var o : gameSet) {
				var g = (JsonArray) o;
				int rule = g.get(0).getAsInt();
				int map = g.get(0).getAsInt();
				int flags = g.get(0).getAsInt();

				bo.writeByte(rule).writeByte(map).writeByte(flags);
			}

			bo.writeZero(0xd5 - bo.writerIndex());

			bo.writeZero(5).writeBytes(wr).writeByte(game.getMaxPlayers()).writeByte(numPlayers).writeInt(briefingTime)
					.writeZero(0x16).writeByte(game.getStance()).writeByte(levelLimitTolerance).writeInt(0x16)
					.writeInt(sneTime).writeInt(sneRounds).writeInt(capTime).writeInt(capRounds).writeInt(resTime)
					.writeInt(resRounds).writeInt(tdmTime).writeInt(tdmRounds).writeInt(tdmTickets).writeInt(dmTime)
					.writeInt(dmTickets).writeInt(baseTime).writeInt(baseRounds).writeInt(bombTime).writeInt(bombRounds)
					.writeInt(tsneTime).writeInt(tsneRounds);

			if (uniquesRandom) {
				bo.writeByte(0x80 + uniqueRed).writeByte(0x80 + uniqueBlue);
			} else {
				bo.writeByte(uniqueRed).writeByte(uniqueBlue);
			}

			bo.writeZero(7).writeByte(commonA).writeByte(commonB).writeZero(1).writeShort(idleKick)
					.writeShort(teamKillKick).writeInt(0x2e).writeBoolean(capExtraTime).writeByte(sneSnake)
					.writeByte(sdmTime).writeByte(sdmRounds).writeByte(intTime).writeByte(dmRounds).writeByte(scapTime)
					.writeByte(scapRounds).writeByte(raceTime).writeByte(raceRounds).writeZero(1)
					.writeByte(hostOptionsExtraTimeFlags).writeZero(4);

			// Write players
			try {
				playersLock.readLock().lock();

				// Write host
				for (var player : players) {
					var chara = player.getChara();

					if (chara == game.getHost()) {
						bo.writeInt(chara.getId());
						Buffers.writeStringFill(bo, Util.getFullCharacterName(chara), 16);
						bo.writeInt(player.getPing());

						int exp = 0;
						bo.writeInt(exp);

						break;
					}
				}

				// Write rest
				for (var player : players) {
					var chara = player.getChara();

					if (chara != game.getHost()) {
						bo.writeInt(chara.getId());
						Buffers.writeStringFill(bo, Util.getFullCharacterName(chara), 16);
						bo.writeInt(player.getPing());

						int exp = 0;
						bo.writeInt(exp);
					}
				}
			} finally {
				playersLock.readLock().unlock();
			}

			// Write payload
			ctx.write(new Packet(0x4313, bo));
		} catch (Exception e) {
			logger.error("getDetails- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4313, error);
		}
	}

	/**
	 * GameErrors for 4321:
	 * 
	 * -0x1f7 = Maximum number of characters already reached.
	 * 
	 * -0x21c = Password is incorrect.
	 * 
	 * -0x21d = You are currently banned from creating and joining games.
	 * 
	 * -0x21e = This Combat Training session is not currently accepting applicants.
	 */
	public static void join(ChannelHandlerContext ctx) {
		GameError error = null;
		ByteBuf bo = null;
		try {
			// Get session user
			var user = LocalUsers.get(ctx.channel());
			if (user == null) {
				logger.error("join- Invalid session.");
				error = GameError.INVALID_SESSION;
				return;
			}
			
			// GAME_FULL
			// GAME_WRONG_PASSWORD
			// GAME_JOIN_BANNED
			// GAME_TRAINING_CLOSED
			
			logger.error("join- Test error.");
			error = GameError.INVALID_SESSION;
			return;
		} catch (Exception e) {
			logger.error("join- Exception occurred.", e);
			error = GameError.GENERAL;
			Buffers.release(bo);
		} finally {
			Packets.writeError(ctx, 0x4321, error);
		}
	}

}
