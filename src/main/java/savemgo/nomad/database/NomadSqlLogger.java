package savemgo.nomad.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;

public class NomadSqlLogger implements SqlLogger {

	private static final Logger logger = LogManager.getLogger();

	public void logBeforeExecution(StatementContext context) {
		logger.debug(context.getRenderedSql());
	}

	public void logAfterExecution(StatementContext context) {

	}

	public void logException(StatementContext context) {

	}

}
