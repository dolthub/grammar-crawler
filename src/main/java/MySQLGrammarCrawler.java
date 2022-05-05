import java.util.*;

public class MySQLGrammarCrawler {

    private static Set<String> rulesToSkip = new HashSet<>();

    private static Map<String, Rule> ruleMap;

    public static Crawler crawler = new Crawler();


    public static void main(String[] args) throws Exception {
        ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

        Rule rule = null;
        rule = ruleMap.get("createTable");
//        rule = ruleMap.get("dropUndoTablespace");
//        rule = ruleMap.get("dropStatement");


        System.out.println(rule);
//        System.out.println(ruleMap.get("tableRefList"));
//        System.out.println(ruleMap.get("undoTableSpaceOptions"));
//        System.out.println(ruleMap.get("undoTableSpaceOption"));
//        System.out.println(ruleMap.get("tsOptionEngine"));

        // Configure crawling rules...
        // Skipping these rules to simplify the output and to make it easier to plug in identifier tokens
        rulesToSkip.add("dotIdentifier");
        rulesToSkip.add("identifierKeyword");
        rulesToSkip.add("fieldIdentifier");

        // TODO: Disabling these to make crawler work for CreateTable without blowing up the heap
        rulesToSkip.add("procedureAnalyseClause");
        rulesToSkip.add("expr");
        rulesToSkip.add("queryExpression");
        rulesToSkip.add("queryExpressionOrParens");
        rulesToSkip.add("partitionClause");
        rulesToSkip.add("createTableOptions");
        rulesToSkip.add("tableConstraintDef");
        rulesToSkip.add("spatialIndexOption");
        rulesToSkip.add("fulltextIndexOption");


        crawler.startCrawl(rule);

        System.out.println();
        System.out.println("Generated Templates:");
        String statementPrefix = "";

        // TODO: We could probably get rid of TemplateManager now that we have a signal when a templatebuffer
        //       is complete and we could feed the generated templates to the StatementWriter dynamically as
        //       they finish.


//        StatementWriter writer = new StdOutStatementWriter();
        StatementWriter writer = new SQLLogicProtoStatementWriter("sqllogic-test.proto");

        // Plug in valid identifiers
        // TODO: This should be extracted into a separate, configurable interface
        for (TemplateBuffer generatedTemplate : crawler.templateBufferManager.generatedTemplates) {
            if (generatedTemplate.aborted) continue;

            String s = statementPrefix + generatedTemplate.toString();

            s = s.replaceFirst("foo", randomNewTableName());
            int i = 1;
            while (s.contains("foo")) {
                s = s.replaceFirst("foo", "c" + i);
                i++;
            }

            writer.write(s);
        }
    }

    private static int tableNameCounter = 1;

    public static String randomNewTableName() {
        return "t" + Integer.toHexString(tableNameCounter++);
    }

    public static String translateSymbol(String symbolName) {
        // TODO: Load these from the lexer instead of manually defining them...
        switch (symbolName) {
            case "DOT_SYMBOL":
                return ".";
            case "COMMA_SYMBOL":
                return ",";
            case "AT_SIGN_SYMBOL":
                return "@";
            case "EQUAL_OPERATOR":
                return "=";
            case "SEMICOLON_SYMBOL":
                return ";";
            case "OPEN_PAR_SYMBOL":
                return "(";
            case "CLOSE_PAR_SYMBOL":
                return ")";
            case "MULT_OPERATOR":
                return "*";
            case "OPEN_CURLY_SYMBOL":
                return "{";
            case "CLOSE_CURLY_SYMBOL":
                return "}";
        }

        // TODO: All of these identifiers need more granular control for customizing in statements
        switch (symbolName) {
            case "IDENTIFIER":
                return "foo";
            case "BACK_TICK_QUOTED_ID":
                return "`foo`";
            case "SINGLE_QUOTED_TEXT":
                return "'text'";
            case "DOUBLE_QUOTED_TEXT":
                return "\"text\"";
            case "INT_NUMBER":
                return "42";
            case "DECIMAL_NUMBER":
                return "4.2";
        }

        if (symbolName.endsWith("_SYMBOL")) {
            return symbolName.substring(0, symbolName.indexOf("_SYMBOL"));
        }

        return symbolName;
    }

    private static final boolean DEBUG = true;

    public static void processElement(CrawlContext currentContext) {
        Element element = currentContext.elementToProcess;
        TemplateBuffer generatedTemplate = currentContext.generatedTemplate;

        // If a template was aborted while in process, don't process any other
        // elements contributing to that generated template.
        if (generatedTemplate.aborted) return;

//        if (DEBUG) {
//            long heapSize = Runtime.getRuntime().totalMemory();
//            long heapMaxSize = Runtime.getRuntime().maxMemory();
//            System.out.println("Max Heap: " + heapMaxSize);
//            System.out.println("Current Heap: " + heapSize);
//        }

        if (element.isOptional()) {
            if (!currentContext.includeOptional) {
                // Here is where we fork off another separate crawler thread, including its own buffer to track its unique output
                CrawlContext newContext = crawler.forkCrawl(currentContext, element);
                newContext.includeOptional = true;
                newContext.parentPath.addAll(currentContext.parentPath);

                if (currentContext.futureElements.isEmpty() == false) {
                    CrawlContext.FutureElementContext futureElementContext = currentContext.futureElements.pop();
                    CrawlContext crawlContext = crawler.continueCrawl(currentContext, futureElementContext.element);
                    crawlContext.parentPath.addAll(futureElementContext.parentPath);
                } else {
                    // At this point, we know the current template is fully complete and done generating
//                    System.out.println(" : " + currentContext.generatedTemplate);
                }

                // Then return to avoid any processing this optional element on this fork
                return;
            }
        }

        if (element instanceof LiteralElement) {
            generatedTemplate.addElement(element);
        } else if (element instanceof ElementGroup) {
            ElementGroup group = (ElementGroup) element;

            List<Element> choices = new ArrayList<>();
            ElementGroup currentGroup = new ElementGroup();
            boolean isChoice = false;
            for (Element e : group.elements) {
                if (e instanceof SeparatorElement) {
                    isChoice = true;
                    choices.add(currentGroup);
                    currentGroup = new ElementGroup();
                } else {
                    currentGroup.elements.add(e);
                }
            }
            if (currentGroup.elements.isEmpty() == false) {
                choices.add(currentGroup);
                currentGroup = null;
            }

            if (isChoice) {
                boolean firstChoice = true;
                for (Element e : choices) {
                    if (firstChoice) {
                        // The first choice uses the current template buffer.
                        // All additional choices get a forked template buffer.
                        CrawlContext crawlContext = crawler.continueCrawl(currentContext, e);
                        crawlContext.parentPath.addAll(currentContext.parentPath);
                        firstChoice = false;
                    } else {
                        // all other choices get a new/forked template buffer
                        CrawlContext crawlContext = crawler.forkCrawl(currentContext, e);
                        crawlContext.parentPath.addAll(currentContext.parentPath);
                    }
                }
                return;
            } else {
                CrawlContext newContext = crawler.continueCrawl(currentContext, group.elements.get(0));
                newContext.parentPath.addAll(currentContext.parentPath);
                for (int i = group.elements.size() - 1; i >= 1; i--) {
                    Element e = group.elements.get(i);
                    CrawlContext.FutureElementContext futureElementContext = new CrawlContext.FutureElementContext(e);
                    futureElementContext.parentPath.addAll(currentContext.parentPath);
                    newContext.futureElements.push(futureElementContext);
                }
                return;
            }
        } else if (element instanceof RuleRefElement) {
            RuleRefElement ruleref = (RuleRefElement) element;
            Rule rule = ruleMap.get(ruleref.getName());

            if (rulesToSkip.contains(rule.name)) {
                generatedTemplate.abort(); // TODO: Should this be a method on the current context?
                return;
            }

            if (currentContext.parentPath.contains(rule.name)) {
                // TODO: We need better control of cycles... for example, instead
                //       of restricting any cycles, we may want to allow one cycle, but not two, per rule.
                generatedTemplate.abort();
                return;
            }

            currentContext.parentPath.add(rule.name);

            boolean first = true;
            for (Alternative alternative : rule.alternatives) {
                if (first) {
                    CrawlContext newContext = crawler.continueCrawl(currentContext, alternative.elements.get(0));
                    newContext.parentPath.addAll(currentContext.parentPath);
                    first = false;
                } else {
                    // dataType explodes our crawl too much, so if we hit this rule, only include the first alternative
                    if (rule.name.equals("dataType") == false) continue;
                    // TODO: For now, we skip following rule alternatives (other than the first alternative), since
                    //       it explodes the crawl space and blows the heap.
//                    CrawlContext newContext = forkCrawl(currentContext, alternative.elements.get(0));
//                    newContext.parentPath.addAll(currentContext.parentPath);
                }
            }
            return;
        } else {
            throw new RuntimeException("Unexpected element type: " + element.getClass());
        }

        // TODO: These don't ever even get here because of all the early returns
        if (element.isRepeated()) { // *
            // TODO: For any-number-of-times elements... we want to generate:
            //       one template form without the repeated element
            //       one template form with the repeated element once
            //       one template form with the repeated element twice
            // TODO: Processing each element once by default, but controllable through CrawlContext
            //          crawlContext.processCount = 2;
            //          crawlContext.processCount = 0;
            ///      That could enable this code to be pulled up to the start of this method, with optional handling
        } else if (element.isOnceOrMore()) { // +
            // include once-or-more elements just once, for now
            // TODO: for once-or-more elements, we want to generate:
            //       one template form with the element included once
            //       one template form with the element included twice
        }

        // Queue up the next element to be processed from our stack
        if (currentContext.futureElements.isEmpty()) {
            // At this point, we know a rule should be fully complete
//            System.out.println(" : " + currentContext.generatedTemplate);
        } else {
            CrawlContext.FutureElementContext futureElementContext = currentContext.futureElements.pop();
            CrawlContext crawlContext = crawler.continueCrawl(currentContext, futureElementContext.element);
            crawlContext.parentPath.addAll(futureElementContext.parentPath);
        }
    }

    public static class Rules {
        // TODO: Move this up a level, then move all Rule/Alternatives/Elements to this new class?
    }

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
        private List<Element> choices = new ArrayList<>();

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
