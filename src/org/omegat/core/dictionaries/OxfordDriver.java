/**************************************************************************
 Oxford dictionary API plugin for
 OmegaT - Computer Assisted Translation (CAT) tool
          Home page: http://www.omegat.org/

 Copyright (C) 2020-2022 Hiroshi Miura

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
package org.omegat.core.dictionaries;

import org.omegat.util.Language;
import org.omegat.util.Log;
import org.omegat.util.Preferences;
import tokyo.northside.oxfordapi.IOxfordClient;
import tokyo.northside.oxfordapi.OxfordClientException;
import tokyo.northside.oxfordapi.OxfordDictionaryEntry;
import tokyo.northside.oxfordapi.OxfordThreadClient;
import tokyo.northside.oxfordapi.dtd.Entry;
import tokyo.northside.oxfordapi.dtd.Example;
import tokyo.northside.oxfordapi.dtd.LexicalEntry;
import tokyo.northside.oxfordapi.dtd.Pronunciation;
import tokyo.northside.oxfordapi.dtd.Result;
import tokyo.northside.oxfordapi.dtd.Sense;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.omegat.gui.preferences.view.OxfordPreferencesController.getAppId;
import static org.omegat.gui.preferences.view.OxfordPreferencesController.getAppKey;
import static org.omegat.gui.preferences.view.OxfordPreferencesController.isBilingual;
import static org.omegat.gui.preferences.view.OxfordPreferencesController.isMonolingual;

public class OxfordDriver implements IDictionary {

    private final Language source;
    private final Language target;
    private final IOxfordClient searcher;

    public OxfordDriver(final Language source, final Language target) {
        this.source = source;
        this.target = target;
        searcher = new OxfordThreadClient(getAppId(), getAppKey());
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
        if (isMonolingual()) {
            try {
                searcher.queryEntries(words, source.getLanguageCode(), strict).forEach((key, value) -> {
                    for (Result result : value) {
                        for (LexicalEntry lexicalEntry : result.getLexicalEntries()) {
                            dictionaryEntries.add(new DictionaryEntry(
                                    key,
                                    lexicalEntry.getText(),
                                    formatDefinitions(lexicalEntry)));
                        }
                    }
                });
            } catch (OxfordClientException e) {
                Log.log(e);
            }
        }
        if (isBilingual()) {
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

    private String formatDefinitions(LexicalEntry lexicalEntry) {
        String category = lexicalEntry.getLexicalCategory().getText();
        boolean condensed = Preferences.isPreferenceDefault(Preferences.DICTIONARY_CONDENSED_VIEW, false);
        StringBuilder sb = new StringBuilder("[").append(category).append("]&nbsp;");
        for (Entry entry : lexicalEntry.getEntries()) {
            List<Pronunciation> pronunciations = entry.getPronunciations();
            if (pronunciations != null) {
                sb.append("<span>");
                for (Pronunciation pron : pronunciations) {
                    if (pron.getAudioFile() != null) {
                        sb.append("<a href=\"").append(pron.getAudioFile()).append("\">");
                    }
                    sb.append("[").append(pron.getPhoneticSpelling()).append("]");
                    if (pron.getAudioFile() != null) {
                        sb.append("</a>");
                    }
                }
                sb.append("</span>&nbsp;");
            }
            List<String> etymologies = entry.getEtymologies();
            if (etymologies != null) {
                sb.append("<span>");
                for (String etymology : etymologies) {
                    sb.append(etymology);
                }
                sb.append("</span>");
            }
            if (condensed) {
                String delim = "&nbsp;";
                for (Sense sense : entry.getSenses()) {
                    if (sense.getDefinitions() == null) {
                        continue;
                    }
                    for (String text : sense.getDefinitions()) {
                        sb.append("<span class=\"paragraph-start\">").append(delim).append("</span><span>");
                        sb.append(text).append("</span>");
                        delim = "\u00b6";
                    }
                    List<Example> examples = sense.getExamples();
                    if (examples != null) {
                        for (Example ex : examples) {
                            sb.append("<span class=\"paragraph-start\">").append(delim).append("</span><span>");
                            sb.append(ex.getText()).append("</span>");
                        }
                    }
                }
            } else {
                sb.append("<ol>");
                for (Sense sense : entry.getSenses()) {
                    if (sense.getDefinitions() == null) {
                        continue;
                    }
                    for (String text : sense.getDefinitions()) {
                        sb.append("<li>").append(text).append("</li>");
                    }

                    List<Example> examples = sense.getExamples();
                    if (examples != null) {
                        sb.append("<ul>");
                        for (Example ex : examples) {
                            sb.append("<li>").append(ex.getText()).append("</li>");
                        }
                        sb.append("</ul>");
                    }
                }
                sb.append("</ol>");
            }
        }
        return sb.toString();
    }
}
