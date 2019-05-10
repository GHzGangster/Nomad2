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

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.NomadSqlLogger;
import savemgo.nomad.database.record.Lobby;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.packet.ResultError;
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
		ResultError error = ResultError.GENERAL;
		try {
			if (Math.sqrt(1) == 1) {
				error = ResultError.INVALID_SESSION;
				throw new Exception("invalid session");
			}
		} catch (Exception e) {
			logger.error("test: Exception occurred.", e);
			logger.debug("Error: {}", error);
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
