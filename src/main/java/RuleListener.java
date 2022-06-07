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

import parser.ANTLRv4Parser;
import parser.ANTLRv4ParserBaseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RuleListener extends ANTLRv4ParserBaseListener {
    private Rules.Rule currentRule;

    public List<Rules.Rule> allCollectedRules = new ArrayList<>();

    private List<Rules.Alternative> currentRuleAlternatives;

    private Stack<Rules.ElementGroup> stackOfElementGroups = new Stack<>();


    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        currentRule = new Rules.Rule();
    }

    public void exitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        // For a ParserRuleSpecContext, the first child should *always* have the name of the parser rule
        currentRule.setName(ctx.getChild(0).getText());
        allCollectedRules.add(currentRule);
        currentRule = null;
    }

    public void exitAlternative(ANTLRv4Parser.AlternativeContext ctx) {
        // Mark the end of each alternative with a separator element, so we can easily convert
        // the group to a choice later.
        stackOfElementGroups.peek().elements.add(new Rules.SeparatorElement());
    }

    public void enterRuleAltList(ANTLRv4Parser.RuleAltListContext ctx) {
        currentRuleAlternatives = new ArrayList<>();
    }

    public void exitRuleAltList(ANTLRv4Parser.RuleAltListContext ctx) {
        currentRule.alternatives = currentRuleAlternatives;
        currentRuleAlternatives = null;
    }

    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        stackOfElementGroups.push(new Rules.ElementGroup());
    }

    public void exitLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        Rules.Alternative alternative = new Rules.Alternative();
        Rules.ElementGroup group = stackOfElementGroups.pop();

        if (containsMultipleChoices(group)) {
            Rules.Choice choice = convertGroupToChoice(group);
            alternative.elements.add(choice);
        } else {
            removeTrailingSeparator(group);
            alternative.elements.add(group);
        }
        currentRuleAlternatives.add(alternative);
    }

    public void exitTerminal(ANTLRv4Parser.TerminalContext ctx) {
        String s;
        if (ctx.STRING_LITERAL() != null) {
            s = ctx.STRING_LITERAL().getText();
        } else {
            s = ctx.TOKEN_REF().getText();
        }
        Rules.LiteralElement literalElement = new Rules.LiteralElement(s);

        stackOfElementGroups.peek().elements.add(literalElement);
    }

    public void exitRuleref(ANTLRv4Parser.RulerefContext ctx) {
        Rules.RuleRefElement ruleRefElement = new Rules.RuleRefElement(ctx.getText());
        stackOfElementGroups.peek().elements.add(ruleRefElement);
    }

    public void exitEbnfSuffix(ANTLRv4Parser.EbnfSuffixContext ctx) {
        Rules.Element lastElement = stackOfElementGroups.peek().elements.get(stackOfElementGroups.peek().elements.size() - 1);
        applyEbnfSuffix(ctx, lastElement);
    }

    public void enterBlock(ANTLRv4Parser.BlockContext ctx) {
        stackOfElementGroups.push(new Rules.ElementGroup());
    }

    public void exitBlock(ANTLRv4Parser.BlockContext ctx) {
        Rules.ElementGroup group = stackOfElementGroups.pop();

        if (containsMultipleChoices(group)) {
            Rules.Choice choice = convertGroupToChoice(group);
            stackOfElementGroups.peek().elements.add(choice);
        } else {
            removeTrailingSeparator(group);
            stackOfElementGroups.peek().elements.add(group);
        }
    }

    private void removeTrailingSeparator(Rules.ElementGroup group) {
        int lastIndex = group.elements.size() - 1;
        if (group.elements.get(lastIndex) instanceof Rules.SeparatorElement) {
            group.elements.remove(lastIndex);
        }
    }

    private boolean containsMultipleChoices(Rules.ElementGroup group) {
        // Count the number of separators in the group. We expect one separator
        // at the end of the group, so if we see more than one, we know we have
        // multiple choices and should convert this group to a choice.
        int count = 0;
        for (Rules.Element element : group.elements) {
            if (element instanceof Rules.SeparatorElement) count++;
        }
        return count >= 2;
    }

    private Rules.Choice convertGroupToChoice(Rules.ElementGroup group) {
        Rules.Choice choice = new Rules.Choice();
        Rules.ElementGroup newGroup = new Rules.ElementGroup();
        for (Rules.Element element : group.elements) {
            if (element instanceof Rules.SeparatorElement) {
                choice.addElement(newGroup);
                newGroup = new Rules.ElementGroup();
            } else {
                newGroup.elements.add(element);
            }
        }

        return choice;
    }

    private void applyEbnfSuffix(ANTLRv4Parser.EbnfSuffixContext ctx, Rules.Element element) {
        if (ctx.getText().equals("?")) {
            element.isOptional(true);
        } else if (ctx.getText().equals("*")) {
            element.isRepeated(true);
        } else if (ctx.getText().equals("+")) {
            element.isOnceOrMore(true);
        } else {
            throw new RuntimeException("Unexpected EbnfSuffix value: " + ctx.getText());
        }
    }
}
