// Copyright 2022 Dolthub, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Container for the StatementValidator interface and various common implementations.
 */
public class StatementValidators {

    /**
     * A StatementValidator may be optionally supplied to a Cralwer in order to plug in additional validation for
     * generated statements. After each statement is fully generated and reified, it will be sent to the validator
     * and only valid statements will be sent to the main statement writers. Invalid statements will be sent to a
     * separate, optional statement writer.
     */
    public interface StatementValidator {
        /**
         * Applies additional logic to validate a generated statement (e.g. testing it against a real database).
         *
         * @param statement The statement to validate.
         * @return True if the statement is considered valid, false otherwise.
         */
        boolean validateStatement(String statement);
    }

    public static class NoOpStatementValidator implements StatementValidator {
        @Override
        public boolean validateStatement(String statement) {
            return true;
        }
    }

    public static class MySQLStatementValidator implements StatementValidator {
        Connection conn = null;

        public MySQLStatementValidator(String host, int port, String database, String user, String password) throws SQLException {
            String connectionString = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?user=" + user + "&password=" + password;
            conn = DriverManager.getConnection(connectionString);
        }

        /**
         * Returns a new MySQLStatementValidator, configured with the standard SQLLogicTest connection settings.
         *
         * @return A new MySQLStatementValidator, configured with the standard SQLLogicTest connection settings.
         * @throws SQLException If any errors are encountered creating the MySQL connection.
         */
        public static MySQLStatementValidator NewMySQLStatementValidatorForSQLLogicTest() throws SQLException {
            return new MySQLStatementValidator("localhost", 3306, "sqllogictest", "sqllogictest", "password");
        }

        @Override
        public boolean validateStatement(String statement) {
            try {
                conn.createStatement().execute(statement);
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
    }
}
