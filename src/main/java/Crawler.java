import java.util.LinkedList;
import java.util.List;

public class Crawler {
    public List<CrawlContext> contextsToProcess = new LinkedList<>();
    private StatementWriter writer;
    private String prefix = "";

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

    public void startCrawl(Rules.Rule rule) {
        for (Rules.Alternative alternative : rule.alternatives) {
            forkCrawl(null, alternative.elements.get(0));
        }

        start();

        writer.finished();
    }

    public CrawlContext forkCrawl(CrawlContext ctx, Rules.Element elementToProcess) {
        if (ctx == null) ctx = new CrawlContext(null, new TemplateBuffer());

        // Fork off a new TemplateBuffer to write to, so we don't corrupt the previous crawler path
        TemplateBuffer newTemplateBuffer = new TemplateBuffer();
        newTemplateBuffer.addElements(ctx.generatedTemplate.elements);

        CrawlContext newContext = new CrawlContext(elementToProcess, newTemplateBuffer);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    public CrawlContext continueCrawl(CrawlContext ctx, Rules.Element elementToProcess) {
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

            MySQLGrammarCrawler.processElement(ctx);
        }
    }

    public void statementCompleted(TemplateBuffer generatedTemplate) {
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
}
