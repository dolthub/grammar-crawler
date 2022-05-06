public class CrawlStrategies {

    public static interface CrawlStrategy {
        public boolean shouldCrawl();
    }

    public static class FullCrawl implements CrawlStrategy {
        @Override
        public boolean shouldCrawl() {
            return true;
        }
    }

    public static class RandomCrawl implements CrawlStrategy {
        @Override
        public boolean shouldCrawl() {
            return Math.random() > 0.5;
        }
    }
}
