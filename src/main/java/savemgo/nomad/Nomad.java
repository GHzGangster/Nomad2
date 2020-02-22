package savemgo.nomad;

import java.io.BufferedReader;
import java.io.IOException;
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

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.DBLogger;
import savemgo.nomad.database.record.Lobby;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.local.LocalLobby;
import savemgo.nomad.local.util.LocalLobbies;
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
//		int id = 1;
//		String sessionId = "6fd464a1";
//
//		User user = null;
//		Character chara = null;
//		try (Handle handle = DB.open()) {
//			handle.registerRowMapper(BeanMapper.factory(User.class, "u"));
//			handle.registerRowMapper(BeanMapper.factory(Character.class, "c"));
//			handle.registerRowMapper(JoinRowMapper.forTypes(User.class, Character.class));
//
//			var row = handle.createQuery(
//					"SELECT u.id u_id, u.username u_username, u.role u_role, u.banned_until u_banned_until, "
//							+ "u.is_cfw u_iscfw, u.slots u_slots, "
//							+ "c.id c_id, c.user c_user, c.name c_name, c.old_name c_old_name, c.name_prefix c_name_prefix, c.rank c_rank, "
//							+ "c.comment c_comment, c.gameplay_options c_gameplay_options, c.active c_active, "
//							+ "c.creation_time c_creation_time, c.lobby c_lobby "
//							+ "FROM users u JOIN mgo2_characters c ON c.user=u.id WHERE c.id=:id AND u.session=:sessionId")
//					.bind("id", id).bind("sessionId", sessionId).mapTo(JoinRow.class).findOne().orElse(null);
//			if (row != null) {
//				user = row.get(User.class);
//				chara = row.get(Character.class);
//			}
//		}
//
//		if (user != null) {
//			logger.debug("User: {}", user.getUsername());
//		}
//
//		if (chara != null) {
//			logger.debug("Character: {}", Util.getFullCharacterName(chara));
//		}

//		int id = 1;
//
//		try (Handle handle = DB.open()) {
//			handle.registerRowMapper(BeanMapper.factory(Character.class, "c"));
//			handle.registerRowMapper(BeanMapper.factory(CharacterAppearance.class, "a"));
//			handle.registerRowMapper(JoinRowMapper.forTypes(Character.class, CharacterAppearance.class));
//
//			var rows = handle.createQuery("SELECT c.id c_id, c.name c_name, a.gender a_gender, a.head a_head "
//					+ "FROM mgo2_characters c JOIN mgo2_characters_appearance a ON a.chara=c.id WHERE c.user=:user")
//					.bind("user", id).mapTo(JoinRow.class).list();
//			for (var row : rows) {
//				var chara = row.get(Character.class);
//				var appearance = row.get(CharacterAppearance.class);
//
//				if (chara != null) {
//					logger.debug("Character: {}", Util.getFullCharacterName(chara));
//				}
//
//				if (appearance != null) {
//					logger.debug("Appearance head: {}", appearance.getHead());
//				}
//			}
//		}

		byte[] bytes1 = { (byte) 0x71, (byte) 0x69, (byte) 0xD8, (byte) 0x20, (byte) 0x46, (byte) 0x00, (byte) 0x10,
				(byte) 0xF8, (byte) 0x5B, (byte) 0xFC, (byte) 0x37, (byte) 0x19, (byte) 0xBF, (byte) 0xB0, (byte) 0x9E,
				(byte) 0x7C, (byte) 0xB7, (byte) 0xEC, (byte) 0x0B, (byte) 0xA3, (byte) 0xAF, (byte) 0x89, (byte) 0xC2,
				(byte) 0xC0 };

		byte[] bytes2 = { (byte) 0xC3, (byte) 0x5E, (byte) 0x6A, (byte) 0x17, (byte) 0xF4, (byte) 0x37, (byte) 0xA2,
				(byte) 0xCF, (byte) 0xE9, (byte) 0xCB, (byte) 0x85, (byte) 0x2E, (byte) 0x0D, (byte) 0x87, (byte) 0x2C,
				(byte) 0x4B, (byte) 0x05, (byte) 0xDB, (byte) 0xB9, (byte) 0x94, (byte) 0x1D, (byte) 0xBE, (byte) 0x70,
				(byte) 0xF7 };

		byte[] key = Ptsys.decryptKey(Ptsys.KEY_6);

		var bi = Unpooled.wrappedBuffer(bytes1);
		Ptsys.decrypt(key, bi, 0, bi, 0, 0x18);
		System.out.println(ByteBufUtil.prettyHexDump(bi));

		bi = Unpooled.wrappedBuffer(bytes2);
		Ptsys.decrypt(key, bi, 0, bi, 0, 0x18);
		System.out.println(ByteBufUtil.prettyHexDump(bi));
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
		String line = null;
		while (running) {
			try {
				line = bufferedReader.readLine();
			} catch (IOException e) {

			}
			if (line != null) {
				if (line.equals("stop")) {
					running = false;
				} else if (line.equals("test")) {
					test();
				}
			}
		}
	}

	public static void main(String[] args) {
		new Nomad();
	}

}
