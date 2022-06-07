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

import java.text.NumberFormat;
import java.util.Locale;

public class StdOutStatementWriter implements StatementWriter {
    private int statementCount;

    @Override
    public void write(String statement) {
        statementCount++;
        System.out.println(statement);
    }

    @Override
    public void finished() {
        System.out.println();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        System.out.println("Total Statements: " + numberFormat.format(statementCount));
    }
}
