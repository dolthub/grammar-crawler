import java.util.HashSet;
import java.util.Set;

public class CrawlStrategies {

    public static final CrawlStrategy FULL_CRAWL = new FullCrawl();
    public static final CrawlStrategy RANDOM_CRAWL = new RandomCrawl();

    public static interface CrawlStrategy {
        public boolean shouldCrawl(Rules.Element element);
    }

    public static class FullCrawl implements CrawlStrategy {
        @Override
        public boolean shouldCrawl(Rules.Element element) {
            return true;
        }
    }

    public static class RandomCrawl implements CrawlStrategy {
        @Override
        public boolean shouldCrawl(Rules.Element element) {
            return Math.random() > 0.5;
        }
    }
}
