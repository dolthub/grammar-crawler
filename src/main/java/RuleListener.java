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
        // TODO: This is a hack and should be handled better
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
        if (group.elements.get(group.elements.size() - 1) instanceof Rules.SeparatorElement) {
            group.elements.remove(group.elements.size() - 1);
        }
        alternative.elements.add(group);
        currentRuleAlternatives.add(alternative);
    }

    public void exitTerminal(ANTLRv4Parser.TerminalContext ctx) {
        ANTLRv4Parser.TerminalContext foo = (ANTLRv4Parser.TerminalContext) ctx;
        String s;
        if (foo.STRING_LITERAL() != null) {
            s = foo.STRING_LITERAL().getText();
        } else {
            s = foo.TOKEN_REF().getText();
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

        if (containsSeparatorElement(group)) {
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
            stackOfElementGroups.peek().elements.add(choice);
        } else {
            stackOfElementGroups.peek().elements.add(group);
        }
    }

    private boolean containsSeparatorElement(Rules.ElementGroup group) {
        for (Rules.Element element : group.elements) {
            if (element instanceof Rules.SeparatorElement) {
                return true;
            }
        }
        return false;
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
