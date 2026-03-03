package com.example.agrisense360.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EquipmentStoreScraperService {

    private static final int MAX_RESULTS_PER_MARKET = 8;

    private static final Pattern DDG_RESULT_PATTERN = Pattern.compile("<a[^>]*class=\\\"result__a\\\"[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DDG_RESULT_PATTERN_ALT = Pattern.compile("<a[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_TITLE_PATTERN = Pattern.compile("<meta[^>]*property=\\\"og:title\\\"[^>]*content=\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PRICE_META_PATTERN = Pattern.compile("<meta[^>]*property=\\\"product:price:amount\\\"[^>]*content=\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PRICE_TEXT_PATTERN = Pattern.compile("([0-9]{1,3}(?:[., ][0-9]{3})*(?:[.,][0-9]{1,2})?)\\s*(USD|EUR|GBP|TND|MAD|DZD|\\$|€|£)", Pattern.CASE_INSENSITIVE);

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // Reliability + coverage: curated real marketplaces with stable listing pages
    private final List<MarketConfig> marketConfigs = List.of(
        new MarketConfig("TractorHouse", "https://www.tractorhouse.com", "https://www.tractorhouse.com/listings/search?keywords={q}"),
        new MarketConfig("Fastline", "https://www.fastline.com", "https://www.fastline.com/farm-equipment-for-sale/listings?keywords={q}"),
        new MarketConfig("Agriaffaires", "https://www.agriaffaires.com", "https://www.agriaffaires.com/search?keywords={q}"),
        new MarketConfig("Mascus", "https://www.mascus.com", "https://www.mascus.com/agriculture/search?q={q}"),
        new MarketConfig("FarmMachineryLocator", "https://www.farmmachinerylocator.com", "https://www.farmmachinerylocator.com/listings/search?keywords={q}")
    );

    public List<StoreListing> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }

        List<StoreListing> aggregated = new ArrayList<>();
        List<CompletableFuture<List<StoreListing>>> futures = marketConfigs.stream()
            .map(config -> CompletableFuture.supplyAsync(() -> safeSearchMarketProducts(config, trimmed)))
            .toList();

        for (CompletableFuture<List<StoreListing>> future : futures) {
            try {
                aggregated.addAll(future.join());
            } catch (Exception ignored) {
            }
        }

        Map<String, StoreListing> deduped = new LinkedHashMap<>();
        for (StoreListing listing : aggregated) {
            String key = !listing.url.isBlank() ? listing.url : (listing.market + "|" + safeText(listing.title).toLowerCase(Locale.ROOT));
            deduped.putIfAbsent(key, listing);
        }

        List<StoreListing> result = new ArrayList<>(deduped.values());
        if (result.isEmpty()) {
            for (MarketConfig config : marketConfigs) {
                result.add(buildMarketSearchListing(config, trimmed));
            }
        }

        return result.stream()
            .sorted(Comparator.comparing(StoreListing::hasPrice).reversed()
                .thenComparing(StoreListing::priceValue, Comparator.nullsLast(Double::compareTo))
                .thenComparing(StoreListing::title))
            .limit(24)
            .toList();
    }

    private List<StoreListing> safeSearchMarketProducts(MarketConfig config, String query) {
        try {
            return searchMarketProducts(config, query);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<StoreListing> searchMarketProducts(MarketConfig config, String query) {
        List<StoreListing> ddgProducts = searchFromDuckDuckGo(config, query);
        if (!ddgProducts.isEmpty()) {
            return ddgProducts.stream().limit(MAX_RESULTS_PER_MARKET).toList();
        }
        return List.of();
    }

    private List<StoreListing> searchFromDuckDuckGo(MarketConfig config, String query) {
        try {
            String ddgQuery = "site:" + config.baseUrl.replace("https://", "") + " " + query + " agriculture equipment";
            String encoded = URLEncoder.encode(ddgQuery, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encoded;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
                return List.of();
            }

            List<StoreListing> listings = parseDdgResults(response.body(), config, query, DDG_RESULT_PATTERN);
            if (listings.isEmpty()) {
                listings = parseDdgResults(response.body(), config, query, DDG_RESULT_PATTERN_ALT);
            }
            return listings.stream().limit(MAX_RESULTS_PER_MARKET).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<StoreListing> parseDdgResults(String html, MarketConfig config, String query, Pattern pattern) {
        List<StoreListing> listings = new ArrayList<>();
        Matcher matcher = pattern.matcher(html);
        Set<String> terms = tokenize(query);
        Set<String> seen = new LinkedHashSet<>();

        while (matcher.find()) {
            String href = decodeDdgHref(matcher.group(1));
            String title = cleanText(stripTags(matcher.group(2)));
            if (href == null || href.isBlank() || title.isBlank()) {
                continue;
            }
            if (!href.contains(config.baseUrl.replace("https://", ""))) {
                continue;
            }
            if (isIgnoredLink(href)) {
                continue;
            }
            if (!matchesQuery(title + " " + href, terms) && !hasEquipmentHint(title + " " + href)) {
                continue;
            }

            String key = href.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }

            StoreListing detailed = null;
            try {
                detailed = fetchProductPage(config, href, terms);
            } catch (Exception ignored) {
            }
            if (detailed != null) {
                listings.add(detailed);
            } else {
                listings.add(new StoreListing(config.marketName, title, null, "Price on request", href));
            }

            if (listings.size() >= MAX_RESULTS_PER_MARKET) {
                break;
            }
        }
        return listings;
    }

    private String decodeDdgHref(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        int idx = href.indexOf("uddg=");
        if (idx >= 0) {
            String encoded = href.substring(idx + 5);
            int end = encoded.indexOf('&');
            if (end > 0) {
                encoded = encoded.substring(0, end);
            }
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return href;
    }

    private StoreListing buildMarketSearchListing(MarketConfig config, String query) {
        String url = config.searchUrlTemplate.replace("{q}", URLEncoder.encode(query, StandardCharsets.UTF_8));
        String title = "Open " + config.marketName + " search results for: " + query;
        return new StoreListing(config.marketName, title, null, "Browse results", url);
    }

    private StoreListing fetchProductPage(MarketConfig config, String url, Set<String> terms) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
            return null;
        }
        String html = response.body();

        String title = firstMatch(OG_TITLE_PATTERN, html);
        if (title == null || title.isBlank()) {
            title = firstMatch(TITLE_PATTERN, html);
        }
        if (title == null) {
            return null;
        }
        title = cleanText(title);
        if (title.isBlank()) {
            return null;
        }
        String relevanceText = title + " " + url;
        if (!matchesQuery(relevanceText, terms) && !hasEquipmentHint(relevanceText)) {
            return null;
        }

        Double price = null;
        String priceText = "Price on request";

        String priceMeta = firstMatch(PRICE_META_PATTERN, html);
        if (priceMeta != null) {
            price = parsePrice(priceMeta);
        }
        if (price == null) {
            Matcher priceMatcher = PRICE_TEXT_PATTERN.matcher(html);
            if (priceMatcher.find()) {
                String amount = priceMatcher.group(1);
                String currency = priceMatcher.group(2);
                price = parsePrice(amount);
                if (price != null) {
                    priceText = String.format(Locale.US, "%.2f %s", price, currency).trim();
                }
            }
        } else {
            priceText = String.format(Locale.US, "%.2f", price);
        }

        return new StoreListing(config.marketName, title, price, priceText, url);
    }

    private Double parsePrice(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number n) {
                return n.doubleValue();
            }
            String s = String.valueOf(value)
                .replaceAll("[^0-9.,]", "")
                .replace(',', '.');
            if (s.isBlank()) {
                return null;
            }
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isIgnoredLink(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("/login") || lower.contains("/signin") || lower.contains("/register")
            || lower.contains("/privacy") || lower.contains("/terms") || lower.contains("/contact")
            || lower.contains("javascript:") || lower.contains("mailto:") || lower.endsWith("#");
    }

    private boolean hasEquipmentHint(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("tractor") || lower.contains("harvester") || lower.contains("seeder")
            || lower.contains("sprayer") || lower.contains("plow") || lower.contains("combine")
            || lower.contains("farm") || lower.contains("agri") || lower.contains("equipment");
    }

    private String stripTags(String html) {
        if (html == null) {
            return "";
        }
        return cleanText(html.replaceAll("<[^>]+>", " "));
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String firstMatch(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Set<String> tokenize(String query) {
        String[] raw = query.toLowerCase(Locale.ROOT).split("\\s+");
        Set<String> terms = new LinkedHashSet<>();
        for (String term : raw) {
            String clean = term.replaceAll("[^a-z0-9]", "");
            if (!clean.isBlank() && clean.length() >= 2) {
                terms.add(clean);
            }
        }
        return terms;
    }

    private boolean matchesQuery(String title, Set<String> terms) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String term : terms) {
            if (normalized.contains(term)) {
                matches++;
            }
        }
        return matches >= Math.max(1, Math.min(2, terms.size()));
    }

    private record MarketConfig(String marketName, String baseUrl, String searchUrlTemplate) {
    }

    public static class StoreListing {
        private final String market;
        private final String title;
        private final Double priceValue;
        private final String priceText;
        private final String url;
        

        public StoreListing(String market, String title, Double priceValue, String priceText, String url) {
            this.market = market;
            this.title = title;
            this.priceValue = priceValue;
            this.priceText = priceText;
            this.url = url;
        }

        public String market() {
            return market;
        }

        public String title() {
            return title;
        }

        public Double priceValue() {
            return priceValue;
        }

        public String priceText() {
            return priceText;
        }

        public String url() {
            return url;
        }

        public boolean hasPrice() {
            return priceValue != null;
        }
    }
}