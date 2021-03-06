package com.softwareverde.database.postgres.row;

import com.softwareverde.database.Database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresRow implements Database.Row {
    public static PostgresRow fromResultSet(final ResultSet resultSet) {
        final PostgresRow postgresRow = new PostgresRow();

        try {
            final ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 0; i < metaData.getColumnCount(); ++i ) {
                final String columnName = metaData.getColumnLabel(i+1).toLowerCase(); // metaData.getColumnName(i+1);
                final String columnValue = resultSet.getString(i+1);

                postgresRow._columnNames.add(columnName);
                postgresRow._columnValues.put(columnName, columnValue);
            }
        }
        catch (final SQLException e) { }

        return postgresRow;
    }

    protected List<String> _columnNames = new ArrayList<String>();
    protected Map<String, String> _columnValues = new HashMap<String, String>();

    protected PostgresRow() { }

    @Override
    public List<String> getColumnNames() {
        return new ArrayList<String>(_columnNames);
    }

    @Override
    public String getValue(final String columnName) {
        if (! _columnValues.containsKey(columnName)) {
            throw new RuntimeException("Row does not contain column: "+ columnName);
        }

        return _columnValues.get(columnName);
    }
}
