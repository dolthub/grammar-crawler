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

import java.util.HashSet;
import java.util.Set;

/**
 * CrawlStrategies contains CrawlStrategy, the main interface for how the Crawler makes decisions about which paths
 * to traverse in the grammar, as well as common implementations of CrawlStrategy, such as FullCrawl, RandomCrawl, and
 * CoverageAwareCrawl.
 */
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

    @SuppressWarnings("ClassCanBeRecord")
    public static class CoverageAwareCrawl implements CrawlStrategy {
        private final Crawler crawler;

        public CoverageAwareCrawl(Crawler crawler) {
            this.crawler = crawler;
        }

        @Override
        public boolean shouldCrawl(Rules.Element element) {
            // If the current element is parent of literal elements that we haven't
            // used in completed expressions yet, then crawl to increase coverage.
            Set<String> literalElementNames = crawler.findReachableLiteralElements(element);

            for (String literalElementName : literalElementNames) {
                // If we don't have usage info for an element, it means the crawler thinks this element
                // isn't reachable with the current pruned rules, so just continue to the next literal.
                // This can happen if an element that follows this current element is pruned.
                if (crawler.getElementUsage().get(literalElementName) == null) {
                    continue;
                }

                if (crawler.getElementUsage().get(literalElementName) == 0) return true;
            }

            // Otherwise, if a rule contains only literal elements that we've already
            // used, then use a low probability random choice for selection.
            return Math.random() < 0.33;
        }
    }
}
