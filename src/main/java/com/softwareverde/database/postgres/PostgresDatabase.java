package com.softwareverde.database.postgres;

import com.softwareverde.database.Database;
import com.softwareverde.database.postgres.row.PostgresRowFactory;
import com.softwareverde.database.postgres.row.RowFactory;
import com.softwareverde.util.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PostgresDatabase implements Database {
    private RowFactory _rowFactory = new PostgresRowFactory();
    private String _username = "root";
    private String _password = "";
    private String _url = "localhost";
    private String _databaseName = "";
    private Integer _port = 5432;

    private Integer _version = 1;
    private Connection _connection;
    private Boolean _isConnected = false;
    private String _lastInsertId = "-1";

    private String _extractInsertId(final PreparedStatement preparedStatement) {

        try {
            final ResultSet resultSet = preparedStatement.getGeneratedKeys();

            final Integer insertId;
            {
                if (resultSet.next()) {
                    insertId = resultSet.getInt(1);
                }
                else {
                    insertId = null;
                }
            }

            resultSet.close();
            return Util.coalesce(insertId, "-1").toString();
        }
        catch (final SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private PreparedStatement _prepareStatement(final String query, final String[] parameters) {
        try {
            final Boolean isInsert = (query.trim().regionMatches(true, 0, "INSERT", 0, 6));
            final PreparedStatement preparedStatement = _connection.prepareStatement(query, (isInsert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS));
            if (parameters != null) {
                for (int i = 0; i < parameters.length; ++i) {
                    preparedStatement.setString(i+1, parameters[i]);
                }
            }
            return preparedStatement;
        }
        catch (final SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private void _executeSql(final String query, final String[] parameters) {
        try {
            final PreparedStatement preparedStatement = _prepareStatement(query, parameters);
            preparedStatement.execute();
            _lastInsertId = _extractInsertId(preparedStatement);
            preparedStatement.close();
        }
        catch (final SQLException e) {
            _lastInsertId = null;
            e.printStackTrace();
        }
    }

    private void _connect() {
        try {
            Class.forName("org.postgresql.Driver");

            final Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user", _username);
            connectionProperties.setProperty("password", _password);
            // connectionProperties.setProperty("ssl", "true");
            _connection = DriverManager.getConnection("jdbc:postgresql://"+ _url +":"+ _port +"/"+ _databaseName, connectionProperties);
            _isConnected = (_connection != null);
        }
        catch (final Exception exception) {
            _isConnected = false;
        }
    }

    public PostgresDatabase(final String url, final String username, final String password) {
        _url = url;
        _username = username;
        _password = password;
    }

    public void setPort(final Integer port) {
        _port = port;
    }

    public void setDatabase(final String databaseName) {
        _databaseName = databaseName;

        if (_isConnected) {
            try {
                _connection.setCatalog(databaseName);
            }
            catch (final SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void connect() {
        _connect();
    }

    @Override
    public Boolean isConnected() {
        return _isConnected;
    }

    @Override
    public synchronized void executeDdl(final String query) {
        if (! _isConnected) { return; }

        try {
            final Statement statement = _connection.createStatement();
            statement.execute(query);
            statement.close();
        }
        catch (final SQLException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public synchronized Long executeSql(final String query, final String[] parameters) {
        if (! _isConnected) { return 0L; }

        _executeSql(query, parameters);
        return Util.parseLong(_lastInsertId);
    }

    @Override
    public synchronized List<Row> query(final String query, final String[] parameters) {
        if (! _isConnected) { new ArrayList<Row>(); }

        try {
            final PreparedStatement preparedStatement = _prepareStatement(query, parameters);
            final ResultSet resultSet = preparedStatement.executeQuery();

            final List<Row> results = new ArrayList<Row>();
            while (resultSet.next()) {
                results.add(_rowFactory.fromResultSet(resultSet));
            }
            resultSet.close();
            preparedStatement.close();

            return results;
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return new ArrayList<Row>();
        }
    }

    @Override
    public Integer getVersion() {
        return _version;
    }

    @Override
    public void setVersion(final Integer newVersion) {
        _version = newVersion;
    }

    @Override
    public Boolean shouldBeCreated() {
        return false;
    }

    @Override
    public Boolean shouldBeUpgraded() {
        return false;
    }

    @Override
    public Boolean shouldBeDowngraded() {
        return false;
    }

    @Override
    public Database newConnection() {
        final PostgresDatabase newConnection = new PostgresDatabase(_url, _username, _password);
        if (_databaseName != null) {
            newConnection.setDatabase(_databaseName);
        }
        if (_isConnected) {
            newConnection.connect();
        }
        return newConnection;
    }

    @Override
    public void disconnect() {
        try { _connection.close(); } catch (final SQLException e) { }
    }

    /**
     * Require dependencies be packaged at compile-time.
     */
    private static final Class[] UNUSED = {
        org.postgresql.Driver.class
    };
}
