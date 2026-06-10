package jdbc;

import exception.ConnectionException;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public final class DatabaseConnection {

    private DatabaseConnection() {
        throw new UnsupportedOperationException();
    }

    public static DataSource createDataSource(
            final String jdbcUrl,
            final String username,
            final String password
    ) throws ConnectionException {
        try {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL(jdbcUrl);
            dataSource.setUser(username);
            dataSource.setPassword(password);
            return dataSource;
        } catch (Exception e) {
            throw new ConnectionException("Error while configuring data source: " + e.getMessage());
        }
    }
}
