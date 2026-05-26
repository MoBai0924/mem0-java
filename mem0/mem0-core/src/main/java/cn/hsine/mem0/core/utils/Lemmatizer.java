package cn.hsine.mem0.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lemmatization utility for BM25 keyword search.
 * Java equivalent of Python mem0/utils/lemmatization.py.
 *
 * Uses rule-based English lemmatization (no external NLP dependency required).
 * For production use with higher accuracy, consider integrating Stanford CoreNLP.
 *
 * @author MoBai

 */
public final class Lemmatizer {

    private static final Logger log = LoggerFactory.getLogger(Lemmatizer.class);

    private Lemmatizer() {}

    // Common English stop words
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "am", "are", "was", "were", "be",
        "been", "being", "have", "has", "had", "do", "does", "did", "will",
        "would", "could", "should", "may", "might", "shall", "can", "need",
        "dare", "ought", "used", "it", "its", "this", "that", "these", "those",
        "i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you",
        "your", "yours", "yourself", "yourselves", "he", "him", "his",
        "himself", "she", "her", "hers", "herself", "they", "them", "their",
        "theirs", "themselves", "what", "which", "who", "whom", "when",
        "where", "why", "how", "all", "each", "every", "both", "few", "more",
        "most", "other", "some", "such", "no", "nor", "not", "only", "own",
        "same", "so", "than", "too", "very", "just", "because", "as", "until",
        "while", "if", "then", "else", "also", "about", "up", "out", "off",
        "over", "under", "again", "further", "once", "here", "there", "any"
    );

    // Irregular verb lemmas
    private static final Map<String, String> IRREGULAR_LEMMAS = Map.ofEntries(
        Map.entry("ran", "run"), Map.entry("running", "run"),
        Map.entry("ate", "eat"), Map.entry("eating", "eat"),
        Map.entry("went", "go"), Map.entry("going", "go"),
        Map.entry("gone", "go"), Map.entry("came", "come"),
        Map.entry("coming", "come"), Map.entry("took", "take"),
        Map.entry("taking", "take"), Map.entry("taken", "take"),
        Map.entry("gave", "give"), Map.entry("giving", "give"),
        Map.entry("given", "give"), Map.entry("made", "make"),
        Map.entry("making", "make"), Map.entry("said", "say"),
        Map.entry("saying", "say"), Map.entry("told", "tell"),
        Map.entry("telling", "tell"), Map.entry("got", "get"),
        Map.entry("getting", "get"), Map.entry("gotten", "get"),
        Map.entry("knew", "know"), Map.entry("knowing", "know"),
        Map.entry("known", "know"), Map.entry("thought", "think"),
        Map.entry("thinking", "think"), Map.entry("saw", "see"),
        Map.entry("seeing", "see"), Map.entry("seen", "see"),
        Map.entry("wanted", "want"), Map.entry("wanting", "want"),
        Map.entry("liked", "like"), Map.entry("liking", "like"),
        Map.entry("loved", "love"), Map.entry("loving", "love"),
        Map.entry("hated", "hate"), Map.entry("hating", "hate"),
        Map.entry("tried", "try"), Map.entry("trying", "try"),
        Map.entry("felt", "feel"), Map.entry("feeling", "feel"),
        Map.entry("found", "find"), Map.entry("finding", "find"),
        Map.entry("worked", "work"), Map.entry("working", "work"),
        Map.entry("lived", "live"), Map.entry("living", "live"),
        Map.entry("moved", "move"), Map.entry("moving", "move"),
        Map.entry("started", "start"), Map.entry("starting", "start"),
        Map.entry("stopped", "stop"), Map.entry("stopping", "stop"),
        Map.entry("helped", "help"), Map.entry("helping", "help"),
        Map.entry("needed", "need"), Map.entry("needing", "need"),
        Map.entry("used", "use"), Map.entry("using", "use"),
        Map.entry("learned", "learn"), Map.entry("learning", "learn"),
        Map.entry("preferred", "prefer"), Map.entry("preferring", "prefer"),
        Map.entry("recommended", "recommend"), Map.entry("recommending", "recommend"),
        Map.entry("mentioned", "mention"), Map.entry("mentioning", "mention"),
        Map.entry("enjoyed", "enjoy"), Map.entry("enjoying", "enjoy"),
        Map.entry("planned", "plan"), Map.entry("planning", "plan"),
        Map.entry("decided", "decide"), Map.entry("deciding", "decide"),
        Map.entry("chose", "choose"), Map.entry("chosen", "choose"),
        Map.entry("choosing", "choose"), Map.entry("bought", "buy"),
        Map.entry("buying", "buy"), Map.entry("spent", "spend"),
        Map.entry("spending", "spend"), Map.entry("built", "build"),
        Map.entry("building", "build"), Map.entry("wrote", "write"),
        Map.entry("writing", "write"), Map.entry("written", "write"),
        Map.entry("read", "read"), Map.entry("reading", "read"),
        Map.entry("spoke", "speak"), Map.entry("speaking", "speak"),
        Map.entry("spoken", "speak"), Map.entry("drove", "drive"),
        Map.entry("driving", "drive"), Map.entry("driven", "drive"),
        Map.entry("sat", "sit"), Map.entry("sitting", "sit"),
        Map.entry("stood", "stand"), Map.entry("standing", "stand"),
        Map.entry("lost", "lose"), Map.entry("losing", "lose"),
        Map.entry("won", "win"), Map.entry("winning", "win"),
        Map.entry("sent", "send"), Map.entry("sending", "send"),
        Map.entry("brought", "bring"), Map.entry("bringing", "bring"),
        Map.entry("kept", "keep"), Map.entry("keeping", "keep"),
        Map.entry("left", "leave"), Map.entry("leaving", "leave"),
        Map.entry("met", "meet"), Map.entry("meeting", "meet"),
        Map.entry("paid", "pay"), Map.entry("paying", "pay"),
        Map.entry("put", "put"), Map.entry("putting", "put"),
        Map.entry("set", "set"), Map.entry("setting", "set"),
        Map.entry("having", "have"), Map.entry("being", "be")
    );

    // Suffix patterns for rule-based lemmatization
    private static final Pattern ING_PATTERN = Pattern.compile("(.+)ing$");
    private static final Pattern ED_PATTERN = Pattern.compile("(.+)ed$");
    private static final Pattern S_PATTERN = Pattern.compile("(.+)s$");
    private static final Pattern IES_PATTERN = Pattern.compile("(.+)ies$");
    private static final Pattern ES_PATTERN = Pattern.compile("(.+)es$");
    private static final Pattern ER_PATTERN = Pattern.compile("(.+)er$");
    private static final Pattern EST_PATTERN = Pattern.compile("(.+)est$");

    /**
     * todo 有问题
     *
     * Lemmatizes text for BM25 indexing/search.
     * Equivalent of Python lemmatize_for_bm25().
     *
     * @param text the input text
     * @return lemmatized text with stop words removed
     */
    public static String lemmatizeForBm25(String text) {
        if (text == null || text.isBlank()) return "";

        String lower = text.toLowerCase();
        List<String> tokens = tokenize(lower);
        List<String> result = new ArrayList<>();

        for (String token : tokens) {
            if (STOP_WORDS.contains(token)) continue;
            if (!isAlphanumeric(token)) continue;

            String lemma = lemmatize(token);
            if (lemma != null && !lemma.isEmpty() && isAlphanumeric(lemma)) {
                result.add(lemma);
            }

            // Also add original -ing form if it differs from lemma
            // (handles noun/verb ambiguity: "meeting" as noun vs verb)
            if (token.endsWith("ing") && !token.equals(lemma) && isAlphanumeric(token)) {
                result.add(token);
            }
        }

        return String.join(" ", result);
    }

    /**
     * Simple tokenization using BreakIterator.
     */
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        BreakIterator bi = BreakIterator.getWordInstance(Locale.ENGLISH);
        bi.setText(text);
        int start = bi.first();
        for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
            String word = text.substring(start, end).trim();
            if (!word.isEmpty()) tokens.add(word);
        }
        return tokens;
    }

    /**
     * Rule-based lemmatization for a single token.
     */
    static String lemmatize(String token) {
        // Check irregular forms first
        String irregular = IRREGULAR_LEMMAS.get(token);
        if (irregular != null) return irregular;

        // Rule-based suffix stripping
        var ingMatch = ING_PATTERN.matcher(token);
        if (ingMatch.matches()) {
            String stem = ingMatch.group(1);
            // Double consonant: running -> run
            if (stem.length() >= 2 && stem.charAt(stem.length()-1) == stem.charAt(stem.length()-2)) {
                return stem.substring(0, stem.length() - 1);
            }
            // Add 'e' for soft consonant: making -> make, living -> live
            if (stem.length() >= 1) {
                char last = stem.charAt(stem.length() - 1);
                if (last == 'k' || last == 'v' || last == 't' || last == 'd' || last == 'r' || last == 'p' || last == 'm' || last == 'n') {
                    return stem + "e";
                }
            }
            return stem;
        }

        var edMatch = ED_PATTERN.matcher(token);
        if (edMatch.matches()) {
            String stem = edMatch.group(1);
            // Double consonant: stopped -> stop
            if (stem.length() >= 2 && stem.charAt(stem.length()-1) == stem.charAt(stem.length()-2)) {
                return stem.substring(0, stem.length() - 1);
            }
            return stem;
        }

        // Comparatives/superlatives
        var estMatch = EST_PATTERN.matcher(token);
        if (estMatch.matches()) {
            String stem = estMatch.group(1);
            if (stem.endsWith("i")) return stem.substring(0, stem.length()-1) + "y"; // happiest -> happy
            return stem;
        }

        var erMatch = ER_PATTERN.matcher(token);
        if (erMatch.matches()) {
            String stem = erMatch.group(1);
            if (stem.endsWith("i")) return stem.substring(0, stem.length()-1) + "y"; // happier -> happy
            return stem;
        }

        // Plurals
        var iesMatch = IES_PATTERN.matcher(token);
        if (iesMatch.matches()) return iesMatch.group(1) + "y"; // cities -> city

        var esMatch = ES_PATTERN.matcher(token);
        if (esMatch.matches()) {
            String stem = esMatch.group(1);
            if (stem.endsWith("s") || stem.endsWith("x") || stem.endsWith("z") || stem.endsWith("sh") || stem.endsWith("ch")) {
                return stem; // boxes -> box
            }
        }

        var sMatch = S_PATTERN.matcher(token);
        if (sMatch.matches()) {
            String stem = sMatch.group(1);
            if (stem.length() > 2) return stem; // cats -> cat
        }

        return token;
    }

    private static boolean isAlphanumeric(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) return false;
        }
        return !s.isEmpty();
    }
}
