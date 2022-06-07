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

import java.util.ArrayList;
import java.util.List;

/**
 * Rules contain the model for a parsed grammar.
 */
public class Rules {
    public static class Rule {
        public String name;

        public List<Alternative> alternatives = new ArrayList<>();

        public Rule(String name) {
            this.name = name;
        }

        public Rule() {
        }

        public void setName(String s) {
            this.name = s;
        }

        public String toString() {
            String s = name + "\n";
            for (Alternative alternative : alternatives) {
                s += " - " + alternative;
                s += "\n";
            }
            return s;
        }
    }

    public static class Alternative {
        public List<Element> elements = new ArrayList<>();

        public Alternative() {
        }

        public String toString() {
            return elements.toString();
        }
    }

    // Ebnf represents a choice of elements
    public static class Choice extends Element {
        public List<Element> choices = new ArrayList<>();

        public Choice() {
            super("CHOICE");
        }

        public void addElement(Element e) {
            choices.add(e);
        }

        public String toString() {
            String s = "(";

            List<String> choiceStrings = new ArrayList<>();
            for (Element e : choices) {
                choiceStrings.add(e.toString());
            }

            s += String.join(" | ", choiceStrings);

            s += ")";
            return s;
        }
    }

    public static class ElementGroup extends Element {
        List<Element> elements = new ArrayList<>();

        public ElementGroup() {
            super("GROUP");
        }

        public String toString() {
            List<String> strings = new ArrayList<>();
            for (Element e : elements) strings.add(e.toString());
            String s = "(" + String.join(" ", strings) + ")";

            return s + (isOnceOrMore() ? "+" : "") + (isRepeated() ? "*" : "") + (isOptional() ? "?" : "");
        }
    }

    public static class Element {
        private final String s;

        private boolean optional = false;
        private boolean repeated = false;
        private boolean onceOrMore;

        public Element(String s) {
            this.s = s;
        }

        public void isOptional(boolean b) {
            optional = b;
        }

        public boolean isOptional() {
            return optional;
        }

        public void isRepeated(boolean b) {
            repeated = b;
        }

        public boolean isRepeated() {
            return repeated;
        }

        public void isOnceOrMore(boolean b) {
            onceOrMore = b;
        }

        public boolean isOnceOrMore() {
            return onceOrMore;
        }

        public String getName() {
            return s;
        }

        public String toString() {
            return s + (onceOrMore ? "+" : "") + (repeated ? "*" : "") + (optional ? "?" : "");
        }

        @Override
        public boolean equals(Object other) {
            return this.toString().equals(other.toString());
        }

        @Override
        public int hashCode() {
            return this.toString().hashCode();
        }
    }

    public static class SeparatorElement extends Element {
        public SeparatorElement() {
            super("|");
        }
    }

    public static class RuleRefElement extends Element {
        public RuleRefElement(String s) {
            super(s);
        }
    }

    public static class LiteralElement extends Element {
        public LiteralElement(String s) {
            super(s);
        }
    }
}
