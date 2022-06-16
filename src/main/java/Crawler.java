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

import java.util.*;

public class Crawler {
    private final Map<String, Rules.Rule> ruleMap;
    private List<CrawlContext> contextsToProcess = new LinkedList<>();
    private Set<String> rulesToSkip = new HashSet<>();
    private String prefix = "";
    private CrawlStrategies.CrawlStrategy crawlStrategy = CrawlStrategies.FULL_CRAWL;
    private TemplateStats templateStats = new TemplateStats();
    private int statementLimit = -1;
    private StatementWriters.StatementWriter[] writers;
    private Map<String, Integer> mapLiteralElementsToUsage = new HashMap<>();
    private StatementValidators.StatementValidator[] statementValidators;
    private StatementWriters.StatementWriter invalidStatementWriter;
    private EarlyTerminators.EarlyTerminator[] terminators;


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
     * Returns a map of literal element names and the number of times they have been used in
     * completed statement templates.
     *
     * @return A map of literal element names to the number of times they have been used in
     * completed statement templates.
     */
    public Map<String, Integer> getElementUsage() {
        return mapLiteralElementsToUsage;
    }

    /**
     * Sets the output writer for generated statements.
     *
     * @param writer The StatementWriters.StatementWriter to which completed statements should be sent.
     */
    public void setStatementWriter(StatementWriters.StatementWriter writer) {
        this.writers = new StatementWriters.StatementWriter[]{writer};
    }

    /**
     * Sets multiple output writers for generated statements.
     *
     * @param writers The StatementWriters.StatementWriter objects to which completed statements should be sent.
     */
    public void setStatementWriters(StatementWriters.StatementWriter... writers) {
        this.writers = writers;
    }

    /**
     * Sets the optional validator that will be run on generated statements.
     *
     * @param validator The optional validator to use when testing/validating a generated statement.
     */
    public void setStatementValidator(StatementValidators.StatementValidator validator) {
        this.statementValidators = new StatementValidators.StatementValidator[]{validator};
    }

    /**
     * Sets the optional validators that will be run on generated statements.
     *
     * @param validators The optional validators to use when testing/validating a generated statement.
     */
    public void setStatementValidators(StatementValidators.StatementValidator... validators) {
        this.statementValidators = validators;
    }

    /**
     * Sets the optional statement writer to use for invalid statements. If a validator is configured, any
     * statements that don't pass the validator will be sent to this optional statement writer if one is set.
     *
     * @param writer The statement writer to which to send all invalid statements.
     */
    public void setInvalidStatementWriter(StatementWriters.StatementWriter writer) {
        this.invalidStatementWriter = writer;
    }

    /**
     * Sets the optional early statement terminators for this crawler. These terminators allow callers to optimize
     * the crawl by terminating paths early and preventing the crawler from
     *
     * @param terminators The early statement terminators this crawler should use
     */
    public void setEarlyTerminators(EarlyTerminators.EarlyTerminator... terminators) {
        this.terminators = terminators;
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
     * As the crawler completes template statements, it sends them to the configured
     * StatementWriters.StatementWriter for output.
     *
     * @param rule The grammar rule at which to start the crawl.
     */
    public void startCrawl(Rules.Rule rule) {
        initializeUsageMap(rule);

        for (Rules.Alternative alternative : rule.alternatives) {
            forkCrawl(null, alternative.elements.get(0));
        }

        start();

        if (invalidStatementWriter != null) invalidStatementWriter.finished();
        for (StatementWriters.StatementWriter writer : writers) writer.finished();
    }

    /**
     * Prints various statistics on how well the generated statements cover the
     * total available leaf nodes in the grammar's graph.
     */
    public void printCoverageStats() {
        List<String> unusedElements = new LinkedList<>();

        for (String element : mapLiteralElementsToUsage.keySet()) {
            int usageCount = mapLiteralElementsToUsage.get(element);
            if (usageCount == 0) unusedElements.add(element);
        }

        int totalLiteralElementCount = mapLiteralElementsToUsage.size();
        int usedLiteralElementCount = totalLiteralElementCount - unusedElements.size();
        float coveragePercent = (float) usedLiteralElementCount / (float) totalLiteralElementCount;
        System.out.println("Literal Element Coverage: ");
        System.out.println(" - Total:    " + totalLiteralElementCount);
        System.out.println(" - Used:     " + usedLiteralElementCount);
        System.out.println(" - Unused:   " + unusedElements.size());
        System.out.println(" - Coverage: " + String.format("%.02f", (100 * coveragePercent)) + "%");
        System.out.println();

        if (unusedElements.size() > 0) {
            Collections.sort(unusedElements);
            System.out.println("Unused Literal Elements:");
            for (String unusedElement : unusedElements) {
                System.out.println(" - " + unusedElement);
            }
            System.out.println();
        }
    }

    /**
     * Helper method for traversing the grammar from a starting element and returning a set of reachable
     * literal element names.
     *
     * @param element The element in the grammar indicating where to start searching.
     * @return A set of reachable literal element names.
     */
    public Set<String> findReachableLiteralElements(Rules.Element element) {
        List<Rules.Element> elements = new ArrayList<>();
        elements.add(element);
        return findReachableLiteralElements(elements);
    }

    //
    // Private Interface
    //

    /**
     * Traverses the specified grammar rule to find all reachable literal elements and records those
     * in the usage map.
     *
     * @param rule The grammar rule to traverse.
     */
    private void initializeUsageMap(Rules.Rule rule) {
        for (Rules.Alternative alternative : rule.alternatives) {
            for (String literalElement : findReachableLiteralElements(alternative.elements)) {
                mapLiteralElementsToUsage.put(literalElement, 0);
            }
        }
    }

    private Set<String> findReachableLiteralElements(List<Rules.Element> elements) {
        Set<String> foundLiteralElements = new HashSet<>();

        for (Rules.Element element : elements) {
            // If we find a pruned rule, we need to stop processing all other rules in this group,
            // since they won't be reachable, and return an empty set instead of any elements we've
            // already identified since this path is a dead end.
            if (rulesToSkip.contains(element.getName())) {
                // If the element is optional, then continue processing along this path. Otherwise, return
                // an empty set since we can't process this path without including the pruned rule, so any
                // collected results are not actually reachable.
                if (element.isOptional() || element.isRepeated()) continue;
                else return new HashSet<>();
            }

            // TODO: is this pretty much the same logic as CoverageAwareCrawler uses to find literals?
            if (element instanceof Rules.LiteralElement) {
                foundLiteralElements.add(element.getName());
            } else if (element instanceof Rules.ElementGroup) {
                Rules.ElementGroup group = (Rules.ElementGroup) element;
                Set<String> foundElements = findReachableLiteralElements(group.elements);
                foundLiteralElements.addAll(foundElements);
            } else if (element instanceof Rules.Choice) {
                Rules.Choice choice = (Rules.Choice) element;
                // Recurse on each individual choice separately, unlike with ElementGroup, since each choice
                // represents a different option and needs to be looked at independently. e.g. when looking
                // for pruned paths, we need to discard the whole ElementGroup, but only a single Choice.
                for (Rules.Element choiceElement : choice.choices) {
                    Set<String> foundElements = findReachableLiteralElements(choiceElement);
                    foundLiteralElements.addAll(foundElements);
                }
            } else if (element instanceof Rules.RuleRefElement) {
                Rules.RuleRefElement ruleref = (Rules.RuleRefElement) element;
                Rules.Rule rule = ruleMap.get(ruleref.getName());
                // TODO: Eventually we should also track the used rules
                for (Rules.Alternative alternative : rule.alternatives) {
                    Set<String> foundElements = findReachableLiteralElements(alternative.elements);
                    foundLiteralElements.addAll(foundElements);
                }
            } else {
                throw new RuntimeException("Unexpected element type: " + element.getClass());
            }
        }

        return foundLiteralElements;
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
        String statement = StatementReifier.reifyStatement(prefix, generatedTemplate);

        StatementValidators.StatementValidator[] validators = statementValidators != null
                ? statementValidators : new StatementValidators.StatementValidator[]{};

        boolean valid = true;
        for (StatementValidators.StatementValidator validator : validators) {
            if (validator.validateStatement(statement) == false) {
                valid = false;
                break;
            }
        }

        if (valid) {
            for (StatementWriters.StatementWriter writer : writers) writer.write(statement);

            templateStats.completedTemplates++;
            updateLiteralElementUsage(generatedTemplate);
        } else {
            if (invalidStatementWriter != null) invalidStatementWriter.write(statement);
        }
    }

    private void updateLiteralElementUsage(TemplateBuffer generatedTemplate) {
        for (Rules.Element element : generatedTemplate.elements) {
            if (mapLiteralElementsToUsage.containsKey(element.getName()) == false) {
                // If a completed statement includes literal elements that our reachability logic
                // didn't detect, it means there must be a bug in the reachability calculation logic.
                throw new RuntimeException("Element not found in usage map:" + element);
            }
            int usage = mapLiteralElementsToUsage.get(element.getName()) + 1;
            mapLiteralElementsToUsage.put(element.getName(), usage);
        }
    }

    private void processElement(CrawlContext currentContext) {
        Rules.Element element = currentContext.elementToProcess;
        TemplateBuffer generatedTemplate = currentContext.generatedTemplate;

        // Instead of churning through every permutation, we optimize by detecting known invalid patterns
        // early and aborting processing. This allows us to short circuit statements that are syntactically
        // valid, but we know ahead of time they are semantically invalid.
        if (terminators != null) {
            for (EarlyTerminators.EarlyTerminator terminator : terminators) {
                if (terminator.shouldTerminate(generatedTemplate)) {
                    currentContext.abort();
                    return;
                }
            }
        }

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

            boolean noChoiceSelected = true;
            for (Rules.Element e : choice.choices) {
                if (crawlStrategy.shouldCrawl(e) == false) continue;

                if (noChoiceSelected) {
                    // The first choice uses the current template buffer.
                    // All additional choices get a forked template buffer.
                    CrawlContext crawlContext = continueCrawl(currentContext, e);
                    crawlContext.parentPath.addAll(currentContext.parentPath);
                    noChoiceSelected = false;
                } else {
                    // all other choices get a new/forked template buffer
                    CrawlContext crawlContext = forkCrawl(currentContext, e);
                    crawlContext.parentPath.addAll(currentContext.parentPath);
                }
            }
            // If no choice was selected, pick one choice to continue crawling. This is better than aborting
            // the in-progress path since it might include literal tokens that we want coverage over.
            if (noChoiceSelected) {
                int randomIndex = (int) Math.floor(Math.random() * (choice.choices.size()));
                CrawlContext crawlContext = continueCrawl(currentContext, choice.choices.get(randomIndex));
                crawlContext.parentPath.addAll(currentContext.parentPath);
                noChoiceSelected = false;
            }

            return;
        } else if (element instanceof Rules.ElementGroup) {
            Rules.ElementGroup group = (Rules.ElementGroup) element;

            if (group.elements.isEmpty() == false) {
                CrawlContext newContext = continueCrawl(currentContext, group.elements.get(0));
                newContext.parentPath.addAll(currentContext.parentPath);
                for (int i = group.elements.size() - 1; i >= 1; i--) {
                    Rules.Element e = group.elements.get(i);
                    CrawlContext.FutureElementContext futureElementContext = new CrawlContext.FutureElementContext(e);
                    futureElementContext.parentPath.addAll(currentContext.parentPath);
                    newContext.futureElements.push(futureElementContext);
                }
                return;
            }
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
                    if (crawlStrategy.shouldCrawl(alternative.elements.get(0)) == false) continue;

                    CrawlContext newContext = forkCrawl(currentContext, alternative.elements.get(0));
                    newContext.parentPath.addAll(currentContext.parentPath);
                }
            }
            return;
        } else {
            throw new RuntimeException("Unexpected element type: " + element.getClass());
        }

        // TODO: Add support for "zero or more" and "one or more" repeating elements. Currently the code paths
        //       above all exit before this block (except for Rule.Literal), but ideally we would generate
        //       multiple paths through the grammar that exercise these options.
        if (element.isRepeated()) { // *
            // Include zero-or-more elements exactly one time (for now)
        } else if (element.isOnceOrMore()) { // +
            // Include once-or-more elements exactly one time (for now)
        }

        // Queue up the next element to be processed from our stack
        if (currentContext.futureElements.isEmpty() && currentContext.isAborted() == false) {
            // At this point, we know a template should be fully complete
            statementCompleted(currentContext.generatedTemplate);
        } else {
            CrawlContext.FutureElementContext futureElementContext = currentContext.futureElements.pop();
            CrawlContext crawlContext = continueCrawl(currentContext, futureElementContext.element);
            crawlContext.parentPath.addAll(futureElementContext.parentPath);
        }
    }
}
