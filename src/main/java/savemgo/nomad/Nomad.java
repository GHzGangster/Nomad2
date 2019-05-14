package savemgo.nomad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.NomadSqlLogger;
import savemgo.nomad.database.record.Character;
import savemgo.nomad.database.record.Lobby;
import savemgo.nomad.database.record.User;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.server.NomadLobby;
import savemgo.nomad.server.NomadLobbyServer;
import savemgo.nomad.util.Util;

public class Nomad {

	private static Nomad INSTANCE = null;
	private static final Logger logger = LogManager.getLogger();

	private boolean running = false;
	private Config config = new Config();
	private List<NomadLobby> lobbies = new ArrayList<>();

	private Nomad() {

	}

	public static Nomad get() {
		if (Nomad.INSTANCE == null) {
			Nomad.INSTANCE = new Nomad();
		}
		return Nomad.INSTANCE;
	}

	void init() {
		try {
			// Load config
			Path path = Paths.get("config.json");
			String jsonString = Files.readString(path);
			config = Util.MAPPER.readValue(jsonString, Config.class);

			// Init database
			DB.initialize(config);
			DB.getJdbi().setSqlLogger(new NomadSqlLogger());

			if (Math.sqrt(1) == 1) {
				test();
				return;
			}

			// Set up lobbies
			setupLobbies();

			// Start the server
			run();

			logger.debug("Done.");
		} catch (Exception e) {
			logger.error("An error has occurred.", e);
		}
	}

	private void test() {
		int id = 1;
		String sessionId = "9373ac86";

		User user = null;
		Character chara = null;
		try (Handle handle = DB.open()) {
			handle.registerRowMapper(BeanMapper.factory(User.class, "u"));
			handle.registerRowMapper(BeanMapper.factory(Character.class, "c"));
			handle.registerRowMapper(JoinRowMapper.forTypes(User.class, Character.class));
			
			var row = handle.createQuery("SELECT u.id u_id, u.username u_username, u.role u_role, u.banned_until u_banned_until, "
					+ "u.is_cfw u_iscfw, u.slots u_slots, "
					+ "c.id c_id, c.user c_user, c.name c_name, c.old_name c_old_name, c.rank c_rank, c.comment c_comment, "
					+ "c.gameplay_options c_gameplay_options, c.active c_active, c.creation_time c_creation_time, c.lobby c_lobby "
					+ "FROM users u JOIN mgo2_characters c ON c.user=u.id WHERE c.id=:id AND u.session=:sessionId")
					.bind("id", id).bind("sessionId", sessionId).mapTo(JoinRow.class).findOne().orElse(null);
			if (row != null) {				
				user = row.get(User.class);
				chara = row.get(Character.class);
			}
		}

		if (user != null) {
			logger.debug(user.getUsername());
		}

		if (chara != null) {
			logger.debug(chara.getName());
		}
	}

	private void setupLobbies() throws IllegalAccessException, InvocationTargetException {
		// Get lobbies from database
		List<Lobby> dbLobbies = null;
		try (Handle handle = DB.open()) {
			dbLobbies = handle.createQuery("SELECT * FROM mgo2_lobbies WHERE id IN (<ids>)")
					.bindList("ids", config.getLobbies()).mapToBean(Lobby.class).list();
		}

		// Create lobby instances
		for (Lobby dbLobby : dbLobbies) {
			NomadLobby lobby = null;
			if (dbLobby.getType() == 0) {
				lobby = new GateLobby();
			} else if (dbLobby.getType() == 1) {
				lobby = new AccountLobby();
			} else if (dbLobby.getType() == 2) {
				lobby = new GameLobby();
			}

			if (lobby != null) {
				lobby.setId(dbLobby.getId());
				lobby.setType(dbLobby.getType());
				lobby.setSubtype(dbLobby.getSubtype());
				lobby.setName(dbLobby.getName());
				lobby.setIp(dbLobby.getIp());
				lobby.setPort(dbLobby.getPort());
				lobby.setPlayers(dbLobby.getPlayers());

				lobbies.add(lobby);
			}
		}
	}

	private void run() {
		var boss = new NioEventLoopGroup(1);
		var workers = new NioEventLoopGroup();
		try {
			var executors = new DefaultEventExecutorGroup(config.getServerWorkers());

			try {
				start(boss, workers, executors);
			} catch (Exception e) {
				logger.error("Exception occurred while starting.", e);
			}

			try {
				stop();
			} catch (Exception e) {
				logger.error("Exception occurred while stopping.", e);
			}
		} finally {
			workers.shutdownGracefully();
			boss.shutdownGracefully();
		}
	}

	private void start(NioEventLoopGroup boss, NioEventLoopGroup workers, EventExecutorGroup executors) {
		logger.info("Starting...");

		// Start lobby servers
		for (NomadLobby lobby : lobbies) {
			NomadLobbyServer server = new NomadLobbyServer(boss, workers, executors, lobby);
			server.start();
			lobby.setServer(server);

			logger.info("Started lobby {} on {}:{}", lobby.getName(), lobby.getIp(), lobby.getPort());
		}

		running = true;

		// Listen to the console so we can stop gracefully
		startConsoleListener();

		// Sleep while running
		while (running) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Exception occurred while running.", e);
			}
		}
	}

	private void stop() {
		logger.info("Stopping...");

		// Stop lobbies
		for (NomadLobby lobby : lobbies) {
			NomadLobbyServer server = lobby.getServer();
			server.stop();

			logger.info("Stopped lobby {}", lobby.getName());
		}
	}

	private void startConsoleListener() {
		new Thread(() -> {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
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
		}).start();
	}

	public Config getConfig() {
		return config;
	}

	public List<NomadLobby> getLobbies() {
		return lobbies;
	}

}
