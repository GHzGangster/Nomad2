package savemgo.nomad;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.DBLogger;
import savemgo.nomad.database.record.Chara;
import savemgo.nomad.database.record.CharaAppearance;
import savemgo.nomad.database.record.CharaSkills;
import savemgo.nomad.database.record.Clan;
import savemgo.nomad.database.record.Lobby;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.local.LocalChara;
import savemgo.nomad.local.LocalGame;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.local.LocalPlayer;
import savemgo.nomad.local.util.Channels;
import savemgo.nomad.local.util.LocalGames;
import savemgo.nomad.local.util.LocalLobbies;
import savemgo.nomad.local.util.LocalUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.server.LobbyHandler;
import savemgo.nomad.server.LobbyServer;
import savemgo.nomad.util.Util;

public class Nomad {

	private static final Logger logger = LogManager.getLogger();

	private boolean running;
	private Config config;
	private List<LobbyServer> servers = new ArrayList<>();

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private Nomad() {
		try {
			// Load config
			var path = Paths.get("config.json");
			String jsonString = Files.readString(path);
			config = Util.MAPPER.readValue(jsonString, Config.class);

			// Init database
			DB.initialize(config);
			DB.getJdbi().setSqlLogger(new DBLogger());

//			if (Math.sqrt(1) == 1) {
//				test();
//				return;
//			}

			// Set up lobbies and start servers
			var handlers = createLobbyHandlers();
			run(handlers);

			// Stop other services

			logger.debug("Done.");
		} catch (Exception e) {
			logger.error("An error has occurred.", e);
		}
	}

	private void test() {
		try (var handle = DB.open()) {
			handle.registerRowMapper(BeanMapper.factory(Chara.class, "c"));
			handle.registerRowMapper(BeanMapper.factory(CharaAppearance.class, "a"));
			handle.registerRowMapper(BeanMapper.factory(CharaSkills.class, "s"));
			handle.registerRowMapper(BeanMapper.factory(Clan.class, "k"));
			handle.registerRowMapper(
					JoinRowMapper.forTypes(Chara.class, CharaAppearance.class, CharaSkills.class, Clan.class));

			var row = handle.createQuery("SELECT c.comment c_comment, c.rank c_rank, "
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
					+ "k.id k_id, k.name k_name " //
					+ "FROM mgo2_charas c " //
					+ "JOIN mgo2_charas_appearance a ON a.chara=c.id " //
					+ "JOIN mgo2_charas_skills s ON s.chara=c.id " //
					+ "LEFT JOIN mgo2_clans_members m ON m.chara=c.id " //
					+ "LEFT JOIN mgo2_clans k ON m.clan=k.id " //
					+ "WHERE c.id=:chara").bind("chara", 1).mapTo(JoinRow.class).one();

			var chara = row.get(Chara.class);
			var appearance = row.get(CharaAppearance.class);
			var skills = row.get(CharaSkills.class);
			var clan = row.get(Clan.class);

			logger.debug(chara.getRank());
			logger.debug(appearance.getVoice());
			logger.debug(skills.getSkill1());
			logger.debug(clan.getName());
		}
	}

	private List<LobbyHandler> createLobbyHandlers()
			throws IllegalAccessException, InvocationTargetException, IllegalArgumentException {
		var handlers = new ArrayList<LobbyHandler>();

		try (var handle = DB.open()) {
			// Get lobbies from database
			var dbLobbies = handle.createQuery("SELECT * FROM mgo2_lobbies WHERE id IN (<ids>)")
					.bindList("ids", config.getLobbies()).mapToBean(Lobby.class).list();

			// Create lobby instances
			for (var dbLobby : dbLobbies) {
				// TODO: Think about having lobbies on remote servers...
				// We would want a var in LocalLobby to be able to tell.
				// For example, if it's remote, then don't update player count
				// ourselves, query the DB for it instead.
				var localLobby = new LocalLobby();
				localLobby.setId(dbLobby.getId());
				localLobby.setType(dbLobby.getType());
				localLobby.setSubtype(dbLobby.getSubtype());
				localLobby.setName(dbLobby.getName());
				localLobby.setIp(dbLobby.getIp());
				localLobby.setPort(dbLobby.getPort());
				localLobby.setSettings(dbLobby.getSettings());

				LobbyHandler handler = null;
				switch (dbLobby.getType()) {
				case Lobby.TYPE_GATE:
					handler = new GateLobby(localLobby);
					break;
				case Lobby.TYPE_ACCOUNT:
					handler = new AccountLobby(localLobby);
					break;
				case Lobby.TYPE_GAME:
					handler = new GameLobby(localLobby);
					break;
				default:
					throw new IllegalArgumentException("setupLobbies- Unknown lobby type.");
				}

				handlers.add(handler);
			}
		}

		return handlers;
	}

	private void run(List<LobbyHandler> handlers) {
		var boss = new NioEventLoopGroup(1);
		var workers = new NioEventLoopGroup();
		try {
			var executors = new DefaultEventExecutorGroup(config.getServerWorkers());

			// Start lobbies
			try {
				start(boss, workers, executors, handlers);
				running = true;

				// Start console handler
				executor.execute(() -> handleConsole());
			} catch (Exception e) {
				logger.error("Exception occurred while starting.", e);
			}

			// Await stop
			while (running) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error("Exception occurred while running.", e);
				}
			}

			// Stop lobbies
			try {
				stop();
			} catch (Exception e) {
				logger.error("Exception occurred while stopping.", e);
			}
		} finally {
			workers.shutdownGracefully();
			boss.shutdownGracefully();

			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
			}
		}
	}

	private void start(NioEventLoopGroup boss, NioEventLoopGroup workers, EventExecutorGroup executors,
			List<LobbyHandler> handlers) {
		logger.info("Starting...");

		for (LobbyHandler handler : handlers) {
			var server = new LobbyServer(boss, workers, executors, handler);
			server.start();
			servers.add(server);

			LocalLobbies.add(server.getHandler().getLobby());

			logger.info("Started lobby {} on {}:{}", handler.getLobby().getName(), handler.getLobby().getIp(),
					handler.getLobby().getPort());
		}
	}

	private void stop() {
		logger.info("Stopping...");

		for (LobbyServer server : servers) {
			server.stop();

			logger.info("Stopped lobby {}", server.getHandler().getLobby().getName());
		}
	}

	private void handleConsole() {
		var bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		while (running) {
			try {
				String line = bufferedReader.readLine();
				if (line != null) {
					if (line.equals("stop")) {
						running = false;
					} else if (line.equals("test")) {
						test();
					} else if (line.startsWith("error ")) {
						String[] args = line.substring(6).split(" ");
						if (args.length == 1) {
							try {
								int command = Integer.valueOf(args[0], 16);
								Channels.process((ch) -> {
									var user = LocalUsers.get(ch);
									return user != null && user.getId() == 1;
								}, (ch) -> {
									ch.writeAndFlush(new Packet(command, 0xdeadbeef));
								});
							} catch (Exception e) {
								logger.error("exception", e);
							}
						} else if (args.length == 2) {
							try {
								int command = Integer.valueOf(args[0], 16);
								int error = Long.valueOf(args[1], 16).intValue();
								Channels.process((ch) -> {
									var user = LocalUsers.get(ch);
									return user != null && user.getId() == 1;
								}, (ch) -> {
									ch.writeAndFlush(new Packet(command, error));
								});
							} catch (Exception e) {
								logger.error("exception", e);
							}
						} else if (args.length == 3) {
							try {
								int command = Integer.valueOf(args[0], 16);
								int error = Long.valueOf(args[1], 16).intValue();
								int error2 = Long.valueOf(args[2], 16).intValue();
								
								byte[] bytes = new byte[8];
								
								byte[] b = Util.intToBytes(error);
								bytes[0] = b[0];
								bytes[1] = b[1];
								bytes[2] = b[2];
								bytes[3] = b[3];
								
								b = Util.intToBytes(error2);
								bytes[4] = b[0];
								bytes[5] = b[1];
								bytes[6] = b[2];
								bytes[7] = b[3];
								
								Channels.process((ch) -> {
									var user = LocalUsers.get(ch);
									return user != null && user.getId() == 1;
								}, (ch) -> {
									ch.writeAndFlush(new Packet(command, bytes));
								});
							} catch (Exception e) {
								logger.error("exception", e);
							}
						}
					} else if (line.equals("4801")) {
						try {
							byte[] bytes = Files.readAllBytes(Paths.get("4801.bin"));
							
							Channels.process((ch) -> {
								var user = LocalUsers.get(ch);
								return user != null && user.getId() == 1;
							}, (ch) -> {
								ch.writeAndFlush(new Packet(0x4801, bytes));
							});
						} catch (Exception e) {
							logger.error("exception", e);
						}
					} else if (line.equals("flush")) {
						try {
							Channels.process((ch) -> {
								var user = LocalUsers.get(ch);
								return user != null && user.getId() == 1;
							}, (ch) -> {
								ch.flush();
							});
						} catch (Exception e) {
							logger.error("exception", e);
						}
					} else if (line.equals("addgame")) {
						try {
							var lobby = LocalLobbies.get(5);

							var chara = new LocalChara();
							chara.setId(10);
							chara.setActive(true);
							chara.setName("SaveMGO");
							chara.setLobby(lobby);

							var game = new LocalGame();
							game.setId(0x1337);
							game.setHost(chara);
							game.setLobby(lobby);
							game.setName("Test Game");
							game.setMaxPlayers(16);
							game.setGames("[[4,13,0]]");
							game.setCommon(
									"{\"dedicated\":false,\"maxPlayers\":16,\"briefingTime\":1,\"nonStat\":false,\"friendlyFire\":false,\"autoAim\":true,\"uniques\":{\"enabled\":true,\"random\":true,\"red\":4,\"blue\":2},\"enemyNametags\":true,\"silentMode\":false,\"autoAssign\":false,\"teamsSwitch\":true,\"ghosts\":false,\"levelLimit\":{\"enabled\":false,\"base\":0,\"tolerance\":22},\"voiceChat\":true,\"teamKillKick\":2,\"idleKick\":10,\"weaponRestrictions\":{\"enabled\":false,\"primary\":{\"vz\":true,\"p90\":true,\"mp5\":true,\"patriot\":true,\"ak\":true,\"m4\":true,\"mk17\":true,\"xm8\":true,\"g3a3\":true,\"svd\":true,\"mosin\":true,\"m14\":true,\"vss\":true,\"dsr\":true,\"m870\":true,\"saiga\":true,\"m60\":true,\"shield\":true,\"rpg\":true,\"knife\":true},\"secondary\":{\"gsr\":true,\"mk2\":true,\"operator\":true,\"g18\":true,\"mk23\":true,\"de\":true},\"support\":{\"grenade\":true,\"wp\":true,\"stun\":true,\"chaff\":true,\"smoke\":true,\"smoke_r\":true,\"smoke_g\":true,\"smoke_y\":true,\"eloc\":true,\"claymore\":true,\"sgmine\":true,\"c4\":true,\"sgsatchel\":true,\"magazine\":true},\"custom\":{\"suppressor\":true,\"gp30\":true,\"xm320\":true,\"masterkey\":true,\"scope\":true,\"sight\":true,\"laser\":true,\"lighthg\":true,\"lightlg\":true,\"grip\":true},\"items\":{\"envg\":true,\"drum\":true}}}");
							game.setRules(
									"{\"dm\":{\"time\":5,\"rounds\":1,\"tickets\":30},\"tdm\":{\"time\":5,\"rounds\":2,\"tickets\":51},\"res\":{\"time\":7,\"rounds\":2},\"cap\":{\"time\":4,\"rounds\":2,\"extraTime\":false},\"sne\":{\"time\":15,\"rounds\":4,\"snake\":4},\"base\":{\"time\":5,\"rounds\":2},\"bomb\":{\"time\":7,\"rounds\":2},\"tsne\":{\"time\":4,\"rounds\":6},\"sdm\":{\"time\":3,\"rounds\":2},\"int\":{\"time\":20},\"scap\":{\"time\":5,\"rounds\":2,\"extraTime\":true},\"race\":{\"time\":5,\"rounds\":2,\"extraTime\":true}}");
							
							var player = new LocalPlayer();
							player.setGame(game);
							player.setChara(chara);

							var players = game.getPlayers();
							var playersLock = game.getPlayersLock();

							try {
								playersLock.writeLock().lock();
								players.add(player);
							} finally {
								playersLock.writeLock().unlock();
							}

							LocalGames.add(game);
						} catch (Exception e) {
							logger.error("exception", e);
						}
					} else if (line.equals("cleargames")) {
						try {
							LocalGames.getGames().clear();
						} catch (Exception e) {
							logger.error("exception", e);
						}
					}
				}
			} catch (Exception e) {
				logger.error("exception", e);
			}
		}
	}

	public static void main(String[] args) {
		new Nomad();
	}

}
