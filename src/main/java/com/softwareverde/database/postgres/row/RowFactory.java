package com.softwareverde.database.postgres.row;


import java.sql.ResultSet;

public interface RowFactory {
    PostgresRow fromResultSet(final ResultSet resultSet);
}