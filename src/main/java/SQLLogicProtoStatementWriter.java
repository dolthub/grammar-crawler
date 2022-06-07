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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SQLLogicProtoStatementWriter implements StatementWriter {
    private FileWriter fileWriter;

    public SQLLogicProtoStatementWriter(String filename) throws IOException {
        File file = new File(filename);
        fileWriter = new FileWriter(file);
    }

    @Override
    public void write(String statement) {
        try {
            fileWriter.write("statement ok\n");
            fileWriter.write(statement);
            fileWriter.write("\n\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finished() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
