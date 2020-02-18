package savemgo.nomad.database;

import java.beans.PropertyVetoException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import savemgo.nomad.Config;

public class DB {

	private static final Logger logger = LogManager.getLogger();

	private static Jdbi jdbi;
	private static ComboPooledDataSource dataSource;

	public static void initialize(Config config) throws PropertyVetoException {
		dataSource = new ComboPooledDataSource();

		dataSource.setDriverClass("com.mysql.cj.jdbc.Driver");
		dataSource.setJdbcUrl(config.getDatabaseUrl());
		dataSource.setUser(config.getDatabaseUsername());
		dataSource.setPassword(config.getDatabasePassword());

		dataSource.setInitialPoolSize(config.getDatabasePoolMin());
		dataSource.setMinPoolSize(config.getDatabasePoolMin());
		dataSource.setAcquireIncrement(config.getDatabasePoolIncrement());
		dataSource.setMaxPoolSize(config.getDatabasePoolMax());
		dataSource.setNumHelperThreads(config.getDatabaseWorkers());

		dataSource.setTestConnectionOnCheckout(true);

		jdbi = Jdbi.create(dataSource);
	}

	public static Jdbi getJdbi() {
		return jdbi;
	}

	public static Handle open() {
		long time = System.currentTimeMillis();

		Handle handle = jdbi.open();

		time = System.currentTimeMillis() - time;
		if (time >= 1000) {
			logger.debug("open- Took a long time: {} ms", time);
		}

		return handle;
	}

}
