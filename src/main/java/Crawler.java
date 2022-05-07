import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Crawler {
    public List<CrawlContext> contextsToProcess = new LinkedList<>();
    private StatementWriter writer;
    private String prefix = "";

    private CrawlStrategies.CrawlStrategy crawlStrategy = new CrawlStrategies.FullCrawl();

    private TemplateStats templateStats = new TemplateStats();

    private class TemplateStats {
        public int abortedTemplates = 0;
        public int completedTemplates = 0;
    }

    public void setStatementWriter(StatementWriter writer) {
        this.writer = writer;
    }

    public void setStatementPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setCrawlStrategy(CrawlStrategies.CrawlStrategy crawlStrategy) {
        this.crawlStrategy = crawlStrategy;
    }

    public void startCrawl(Rules.Rule rule) {
        for (Rules.Alternative alternative : rule.alternatives) {
            forkCrawl(null, alternative.elements.get(0));
        }

        start();

        writer.finished();
    }

    //
    // Private Interface
    //
    
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
            CrawlContext ctx = contextsToProcess.remove(0);

            // If a crawl context was aborted while in process, don't process any other
            // elements contributing to that generated template.
            if (ctx.isAborted()) continue;

            processElement(ctx);
        }
    }

    private void statementCompleted(TemplateBuffer generatedTemplate) {
        templateStats.completedTemplates++;

        // Plug in valid identifiers and send reified statements out to the statement writer
        String s = prefix + generatedTemplate;

        // TODO: This should be extracted into a separate, configurable interface
        s = s.replaceFirst("foo", StatementReifier.randomNewTableName());
        int i = 1;
        while (s.contains("foo")) {
            s = s.replaceFirst("foo", "c" + i);
            i++;
        }

        writer.write(s);
    }

    private void processElement(CrawlContext currentContext) {
        Rules.Element element = currentContext.elementToProcess;
        TemplateBuffer generatedTemplate = currentContext.generatedTemplate;

        if (element.isOptional()) {
            if (!currentContext.includeOptional) {
                // Here is where we fork off another separate crawler thread, including its own buffer to track its unique output
                if (crawlStrategy.shouldCrawl()) {
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
            generatedTemplate.addElement(element);
        } else if (element instanceof Rules.ElementGroup) {
            Rules.ElementGroup group = (Rules.ElementGroup) element;

            // TODO: This logic for translating an ElementGroup into a list of choices is super hacky.
            //       The parser should take care of this when it analyzes the ANTLR grammar and return
            //       Choice instead of ElementGroup so that we don't have to do any of this here.
            //       But... this is working now, and not the highest priority to fix.
            List<Rules.Element> choices = new ArrayList<>();
            Rules.ElementGroup currentGroup = new Rules.ElementGroup();
            boolean isChoice = false;
            for (Rules.Element e : group.elements) {
                if (e instanceof Rules.SeparatorElement) {
                    isChoice = true;
                    choices.add(currentGroup);
                    currentGroup = new Rules.ElementGroup();
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
                for (Rules.Element e : choices) {
                    if (crawlStrategy.shouldCrawl() == false) continue;

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

                // If the crawl strategy didn't select any choice to crawl, abort the current crawl path
                if (firstChoice) currentContext.abort();

                return;
            } else {
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
            Rules.Rule rule = MySQLGrammarCrawler.ruleMap.get(ruleref.getName());

            // TODO: Instead of just testing contains... we want to have a limit on how many times we recurse through a rule
            //       but... shouldn't the block below prevent cycles?
            if (MySQLGrammarCrawler.rulesToSkip.contains(rule.name)) {
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
                    if (crawlStrategy.shouldCrawl() == false) continue;

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
