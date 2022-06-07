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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import parser.ANTLRv4Lexer;
import parser.ANTLRv4Parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MySQLGrammarUtils {
    public static ANTLRv4Parser parseMySQLGrammar() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("src/main/resources/mysql-grammar/MySQLParser.g4");
        CharStream charStream = CharStreams.fromStream(fileInputStream);
        ANTLRv4Lexer lexer = new ANTLRv4Lexer(charStream);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokenStream);

        return parser;
    }

    public static Map<String, Rules.Rule> loadMySQLGrammarRules() throws IOException {
        ANTLRv4Parser parser = parseMySQLGrammar();
        RuleListener listener = new RuleListener();
        parser.addParseListener(listener);

        ParseTree tree = parser.grammarSpec();
        Map<String, Rules.Rule> ruleMap = new HashMap<>();
        for (Rules.Rule rule : listener.allCollectedRules) {
            ruleMap.put(rule.name, rule);
        }

        return ruleMap;
    }
}
