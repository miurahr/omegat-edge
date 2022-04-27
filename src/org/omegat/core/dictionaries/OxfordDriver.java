package org.omegat.core.dictionaries;

import org.omegat.gui.preferences.view.OxfordPreferencesController;
import org.omegat.util.Language;
import org.omegat.util.Log;
import tokyo.northside.oxfordapi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OxfordDriver implements IDictionary {

    private final Language source;
    private final Language target;
    private final IOxfordClient searcher;

    public OxfordDriver(final Language source, final Language target) {
        this.source = source;
        this.target = target;
        searcher = new OxfordThreadClient(OxfordPreferencesController.getAppId(),
                OxfordPreferencesController.getAppKey());
    }

    /**
     * Read article's text.
     *
     * @param word The word to look up in the dictionary
     * @return List of entries. May be empty, but cannot be null.
     */
    @Override
    public List<DictionaryEntry> readArticles(final String word) {
        return queryArticles(Collections.singletonList(word), true);
    }

    /**
     * Read article's text. Matching is predictive, so e.g. supplying "term"
     * will return articles for "term", "terminology", "termite", etc.
     *
     * @param word The word to look up in the dictionary
     * @return List of entries. May be empty, but cannot be null.
     */
    @Override
    public List<DictionaryEntry> readArticlesPredictive(final String word) {
        return queryArticles(Collections.singletonList(word), false);
    }

    @Override
    public List<DictionaryEntry> retrieveArticles(final Collection<String> words) {
        return queryArticles(words, true);
    }

    @Override
    public List<DictionaryEntry> retrieveArticlesPredictive(final Collection<String> words) {
        return queryArticles(words, false);
    }

    private List<DictionaryEntry> queryArticles(final Collection<String> words, final boolean strict) {
        List<DictionaryEntry> dictionaryEntries = new ArrayList<>();
        if (OxfordPreferencesController.isMonolingual()) {
            try {
                for (OxfordDictionaryEntry en : searcher.getDefinitions(words,
                        source.getLanguageCode(), strict)) {
                    dictionaryEntries.add(new DictionaryEntry(en.getQuery(), en.getWord(), en.getArticle()));
                }
            } catch (OxfordClientException e) {
                Log.log(e);
            }
        }
        if (OxfordPreferencesController.isBilingual()) {
            try {
                for (OxfordDictionaryEntry en : searcher.getTranslations(words,
                        source.getLanguageCode(), target.getLanguageCode())) {
                    dictionaryEntries.add(new DictionaryEntry(en.getQuery(), en.getWord(), en.getArticle()));
                }
            } catch (OxfordClientException e) {
                Log.log(e);
            }
        }
        return dictionaryEntries;
    }

    @Override
    public void close() {
        searcher.close();
    }
}
