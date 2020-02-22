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
		try (var handle = DB.open()) {
			String name = "yolo";
			
			
			
//			handle.useTransaction((h) -> {
//				int id = h.createUpdate("""
//						INSERT INTO mgo2_charas (user, name, creation_time, gameplay_options)
//						VALUES (:user, :name, :creation_time, :gameplay_options)
//						""").bindBean(chara).executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne().orElse(0);
//				
//				int takenId = handle.createQuery("""
//						SELECT id
//						FROM mgo2_charas
//						WHERE name=:name
//						""").bind("name", name).mapTo(Integer.class).findOne().orElse(0);
//				logger.debug(takenId);
//				
//				if (id != 0) {
//					// do appearance
//					
//					h.commit();
//				}
//			});
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
