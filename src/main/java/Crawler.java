import java.util.*;

public class Crawler {
    private final Map<String, Rules.Rule> ruleMap;
    private List<CrawlContext> contextsToProcess = new LinkedList<>();
    private Set<String> rulesToSkip = new HashSet<>();
    private String prefix = "";
    private CrawlStrategies.CrawlStrategy crawlStrategy = CrawlStrategies.FULL_CRAWL;
    private TemplateStats templateStats = new TemplateStats();
    private int statementLimit = -1;
    private StatementWriter[] writers;
    private Map<Rules.Element, Integer> mapLiteralElementsToUsage = new HashMap<>();


    /**
     * Creates a new crawler for the specified grammar rules.
     *
     * @param rules The grammar rules for the crawler to crawl.
     */
    public Crawler(Map<String, Rules.Rule> rules) {
        this.ruleMap = rules;
    }

    /**
     * Returns the map of rules, keyed by their name.
     *
     * @return The map of rules, keyed by their name.
     */
    public Map<String, Rules.Rule> getRuleMap() {
        return ruleMap;
    }

    /**
     * Returns a set of the rule names that the crawler will skip when it encounters them.
     *
     * @return A set of the rule names that the crawler will skip when it encounters them.
     */
    public Set<String> getRulesToSkip() {
        return rulesToSkip;
    }

    /**
     * Returns a map of rule elements and the number of times they have been used in
     * completed statement templates.
     *
     * @return A map of rule elements to the number of times they have been used in
     * completed statement templates.
     */
    public Map<Rules.Element, Integer> getElementUsage() {
        return mapLiteralElementsToUsage;
    }

    /**
     * Sets the output writer for generated statements.
     *
     * @param writer The StatementWriter to which completed statements should be sent.
     */
    public void setStatementWriter(StatementWriter writer) {
        this.writers = new StatementWriter[]{writer};
    }

    /**
     * Sets multiple output writers for generated statements.
     *
     * @param writers The StatementWriter objects to which completed statements should be sent.
     */
    public void setStatementWriters(StatementWriter... writers) {
        this.writers = writers;
    }

    /**
     * Sets the prefix for generated statements. This is useful when starting a crawl on a rule that will
     * not generate a complete/valid statement because of a missing prefix.
     *
     * @param prefix The statement prefix to apply to all crawled statements.
     */
    public void setStatementPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sets the maxiumum number of statements to generate.
     *
     * @param statementLimit The max number of statements to generate.
     */
    public void setStatementLimit(int statementLimit) {
        this.statementLimit = statementLimit;
    }

    /**
     * Sets the CrawlStrategy that controls how this crawler will traverse the grammar graph. The default
     * CrawlStrategy is to perform a full crawl of all paths through the graph, however for anything but
     * simple rules, this will result in a large amount of generated statements.
     *
     * @param crawlStrategy The CrawlStrategy used to control which paths the crawler traverses in the grammar graph.
     */
    public void setCrawlStrategy(CrawlStrategies.CrawlStrategy crawlStrategy) {
        this.crawlStrategy = crawlStrategy;
    }

    /**
     * Adds the specified rule names to the list of rules that the crawler should not crawl. If the crawler
     * encounters any of these rules while crawling the grammar's graph, it will abandon the current crawl path.
     *
     * @param rules The names of rules to add to the skipped rule list.
     */
    public void addRulesToSkip(String... rules) {
        for (String rule : rules) rulesToSkip.add(rule);
    }

    /**
     * Starts the crawler at the specified rule using the crawl strategy set in SetCrawlStrategy.
     * As the crawler completes template statements, it sends them to the configured StatementWriter for output.
     *
     * @param rule The grammar rule at which to start the crawl.
     */
    public void startCrawl(Rules.Rule rule) {
        initializeUsageMap(rule);

        for (Rules.Alternative alternative : rule.alternatives) {
            forkCrawl(null, alternative.elements.get(0));
        }

        start();

        for (StatementWriter writer : writers) writer.finished();
    }

    /**
     * Prints various statistics on how well the generated statements cover the
     * total available leaf nodes in the grammar's graph.
     */
    public void printCoverageStats() {
        List<Rules.Element> unusedElements = new LinkedList<>();
        List<Rules.Element> frequentlyUsedElements = new LinkedList<>();

        for (Rules.Element element : mapLiteralElementsToUsage.keySet()) {
            int usage = mapLiteralElementsToUsage.get(element);
            if (usage == 0) unusedElements.add(element);
            else if (usage > 100) frequentlyUsedElements.add(element);
        }

        int totalLiteralElementCount = mapLiteralElementsToUsage.size();
        int usedLiteralElementCount = totalLiteralElementCount - unusedElements.size();
        float coveragePercent = (float) usedLiteralElementCount / (float) totalLiteralElementCount;
        System.out.println("Literal Element Coverage: ");
        System.out.println(" - Total:    " + totalLiteralElementCount);
        System.out.println(" - Used:     " + usedLiteralElementCount);
        System.out.println(" - Unused:   " + unusedElements.size());
        System.out.println(" - Frequent: " + frequentlyUsedElements.size());
        System.out.println(" - Coverage: " + String.format("%.02f", (100 * coveragePercent)) + "%");
        System.out.println();

        if (unusedElements.size() > 0) {
            System.out.println("Unused Literal Elements:");
            for (Rules.Element unusedElement : unusedElements) {
                System.out.println(" - " + unusedElement.getName());
            }
            System.out.println();
        }
    }

    //
    // Private Interface
    //

    private void initializeUsageMap(Rules.Rule rule) {
        for (Rules.Alternative alternative : rule.alternatives) {
            recurseOnElementsToInitializeRuleMap(alternative.elements);
        }
    }

    private void recurseOnElementsToInitializeRuleMap(List<Rules.Element> elements) {
        for (Rules.Element element : elements) {
            if (mapLiteralElementsToUsage.containsKey(element)) continue;
            if (rulesToSkip.contains(element.getName())) continue;

            if (element instanceof Rules.LiteralElement) {
                mapLiteralElementsToUsage.put(element, 0);
            } else if (element instanceof Rules.ElementGroup) {
                Rules.ElementGroup group = (Rules.ElementGroup) element;
                recurseOnElementsToInitializeRuleMap(group.elements);
            } else if (element instanceof Rules.Choice) {
                Rules.Choice choice = (Rules.Choice) element;
                recurseOnElementsToInitializeRuleMap(choice.choices);
            } else if (element instanceof Rules.RuleRefElement) {
                Rules.RuleRefElement ruleref = (Rules.RuleRefElement) element;
                Rules.Rule rule = ruleMap.get(ruleref.getName());
                // TODO: Eventually we should also track the used rules
                for (Rules.Alternative alternative : rule.alternatives) {
                    recurseOnElementsToInitializeRuleMap(alternative.elements);
                }
            } else {
                throw new RuntimeException("Unexpected element type: " + element.getClass());
            }
        }
    }

    private class TemplateStats {
        // TODO: Crawler doesn't currently have a hook into aborted templates, so we can't track this yet
        public int abortedTemplates = 0;
        public int completedTemplates = 0;
    }

    private CrawlContext forkCrawl(CrawlContext ctx, Rules.Element elementToProcess) {
        if (ctx == null) ctx = new CrawlContext(null, new TemplateBuffer());

        // Fork off a new TemplateBuffer to write to, so we don't corrupt the previous crawler path
        TemplateBuffer newTemplateBuffer = new TemplateBuffer();
        newTemplateBuffer.addElements(ctx.generatedTemplate.elements);

        CrawlContext newContext = new CrawlContext(elementToProcess, newTemplateBuffer);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    private CrawlContext continueCrawl(CrawlContext ctx, Rules.Element elementToProcess) {
        CrawlContext newContext = new CrawlContext(elementToProcess, ctx.generatedTemplate);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    private void start() {
        while (!contextsToProcess.isEmpty()) {
            if (statementLimit > -1 && templateStats.completedTemplates >= statementLimit) {
                break;
            }

            CrawlContext ctx = contextsToProcess.remove(0);

            // If a crawl context was aborted while in process, don't process any other
            // elements contributing to that generated template.
            if (ctx.isAborted()) continue;

            processElement(ctx);
        }
    }

    private void statementCompleted(TemplateBuffer generatedTemplate) {
        templateStats.completedTemplates++;
        updateLiteralElementUsage(generatedTemplate);

        String s = StatementReifier.reifyStatement(prefix, generatedTemplate);
        for (StatementWriter writer : writers) writer.write(s);
    }

    private void updateLiteralElementUsage(TemplateBuffer generatedTemplate) {
        // TODO: This is only going to track the usage of LiteralElements, since
        //       that's all TemplateBuffer will ever contain (the leaf nodes in the graph).
        for (Rules.Element element : generatedTemplate.elements) {
            if (mapLiteralElementsToUsage.containsKey(element) == false) {
                throw new RuntimeException("Element not found in usage map!" + element);
            }
            int usage = mapLiteralElementsToUsage.get(element) + 1;
            mapLiteralElementsToUsage.put(element, usage);
        }
    }

    private void processElement(CrawlContext currentContext) {
        Rules.Element element = currentContext.elementToProcess;
        TemplateBuffer generatedTemplate = currentContext.generatedTemplate;

        if (element.isOptional()) {
            if (!currentContext.includeOptional) {
                // Here is where we fork off another separate crawler thread, including its own buffer to track its unique output
                if (crawlStrategy.shouldCrawl(element)) {
                    CrawlContext newContext = forkCrawl(currentContext, element);
                    newContext.includeOptional = true;
                    newContext.parentPath.addAll(currentContext.parentPath);
                }

                if (currentContext.futureElements.isEmpty() == false) {
                    CrawlContext.FutureElementContext futureElementContext = currentContext.futureElements.pop();
                    CrawlContext crawlContext = continueCrawl(currentContext, futureElementContext.element);
                    crawlContext.parentPath.addAll(futureElementContext.parentPath);
                } else {
                    // At this point, we know the current template is fully complete and done generating
                    statementCompleted(currentContext.generatedTemplate);
                }

                // Then return to avoid any processing this optional element on this fork
                return;
            }
        }

        if (element instanceof Rules.LiteralElement) {
            if (rulesToSkip.contains(element.getName())) {
                currentContext.abort();
                return;
            }

            generatedTemplate.addElement(element);
        } else if (element instanceof Rules.Choice) {
            Rules.Choice choice = (Rules.Choice) element;

            boolean firstChoice = true;
            for (Rules.Element e : choice.choices) {
                if (crawlStrategy.shouldCrawl(element) == false) continue;

                if (firstChoice) {
                    // The first choice uses the current template buffer.
                    // All additional choices get a forked template buffer.
                    CrawlContext crawlContext = continueCrawl(currentContext, e);
                    crawlContext.parentPath.addAll(currentContext.parentPath);
                    firstChoice = false;
                } else {
                    // all other choices get a new/forked template buffer
                    CrawlContext crawlContext = forkCrawl(currentContext, e);
                    crawlContext.parentPath.addAll(currentContext.parentPath);
                }
            }

            // If no paths were selected to be crawled, abort the current crawl path
            if (firstChoice) currentContext.abort();

            return;
        } else if (element instanceof Rules.ElementGroup) {
            Rules.ElementGroup group = (Rules.ElementGroup) element;

            CrawlContext newContext = continueCrawl(currentContext, group.elements.get(0));
            newContext.parentPath.addAll(currentContext.parentPath);
            for (int i = group.elements.size() - 1; i >= 1; i--) {
                Rules.Element e = group.elements.get(i);
                CrawlContext.FutureElementContext futureElementContext = new CrawlContext.FutureElementContext(e);
                futureElementContext.parentPath.addAll(currentContext.parentPath);
                newContext.futureElements.push(futureElementContext);
            }
            return;
        } else if (element instanceof Rules.RuleRefElement) {
            Rules.RuleRefElement ruleref = (Rules.RuleRefElement) element;
            Rules.Rule rule = ruleMap.get(ruleref.getName());

            // TODO: Instead of just testing contains... we want to have a limit on how many times we recurse through a rule
            //       but... shouldn't the block below prevent cycles?
            if (rulesToSkip.contains(rule.name)) {
                currentContext.abort();
                return;
            }

            if (currentContext.parentPath.contains(rule.name)) {
                // TODO: We need better control of cycles... for example, instead
                //       of restricting any cycles, we may want to allow one cycle, but not two, per rule.
                currentContext.abort();
                return;
            }

            currentContext.parentPath.add(rule.name);

            boolean first = true;
            for (Rules.Alternative alternative : rule.alternatives) {
                if (first) {
                    CrawlContext newContext = continueCrawl(currentContext, alternative.elements.get(0));
                    newContext.parentPath.addAll(currentContext.parentPath);
                    first = false;
                } else {
                    // Check the crawl strategy after we've already scheduled the first alternative to be
                    // crawled to ensure at least one gets selected. Ideally the crawl strategy would
                    // be more intelligent and ensure at least one alternative is chosen.
                    if (crawlStrategy.shouldCrawl(element) == false) continue;

                    CrawlContext newContext = forkCrawl(currentContext, alternative.elements.get(0));
                    newContext.parentPath.addAll(currentContext.parentPath);
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
            // At this point, we know a template should be fully complete
            statementCompleted(currentContext.generatedTemplate);
        } else {
            CrawlContext.FutureElementContext futureElementContext = currentContext.futureElements.pop();
            CrawlContext crawlContext = continueCrawl(currentContext, futureElementContext.element);
            crawlContext.parentPath.addAll(futureElementContext.parentPath);
        }
    }
}
