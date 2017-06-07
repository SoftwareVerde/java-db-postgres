package com.softwareverde.database.postgres.row;

import java.sql.ResultSet;

public class PostgresRowFactory implements RowFactory {
    public PostgresRow fromResultSet(final ResultSet resultSet) {
        return PostgresRow.fromResultSet(resultSet);
    }
}