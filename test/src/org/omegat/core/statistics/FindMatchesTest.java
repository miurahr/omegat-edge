/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2021 Hiroshi Miura
               Home page: http://www.omegat.org/
               Support center: https://omegat.org/support

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

package org.omegat.core.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import org.omegat.core.Core;
import org.omegat.core.data.EntryKey;
import org.omegat.core.data.ExternalTMFactory;
import org.omegat.core.data.ExternalTMX;
import org.omegat.core.data.IProject;
import org.omegat.core.data.ITMXEntry;
import org.omegat.core.data.NotLoadedProject;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.ProjectTMX;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.events.IStopped;
import org.omegat.core.matching.NearString;
import org.omegat.core.segmentation.SRX;
import org.omegat.core.segmentation.Segmenter;
import org.omegat.tokenizer.DefaultTokenizer;
import org.omegat.tokenizer.ITokenizer;
import org.omegat.tokenizer.LuceneEnglishTokenizer;
import org.omegat.util.Language;
import org.omegat.util.OConsts;
import org.omegat.util.Preferences;
import org.omegat.util.TestPreferencesInitializer;


public class FindMatchesTest {

    /**
     * Reproduce and test for RFE#1578.
     *
     * When external TM has different target language, and
     * source has country code such as "en-US", and
     * project source is only language code such as "en",
     * and set preference to use other target language,
     * OmegaT show the source of "en-US" as reference.
     *
     * @throws Exception when error occurred.
     */
    @Test
    public void testSearchRFE1578() throws Exception {
        ProjectProperties prop;
        File file = new File("test/data/tmx/en-US_sr.tmx");
        Path tmpDir = Files.createTempDirectory("omegat");
        assertTrue(tmpDir.toFile().isDirectory());
        Core.initializeConsole(new TreeMap<>());
        TestPreferencesInitializer.init();
        Preferences.setPreference(Preferences.EXT_TMX_SHOW_LEVEL2, false);
        Preferences.setPreference(Preferences.EXT_TMX_USE_SLASH, false);
        Preferences.setPreference(Preferences.EXT_TMX_KEEP_FOREIGN_MATCH, true);
        Core.registerTokenizerClass(DefaultTokenizer.class);
        Core.registerTokenizerClass(LuceneEnglishTokenizer.class);
        prop = new ProjectProperties(tmpDir.toFile());
        prop.setSourceLanguage("en");
        prop.setTargetLanguage("cnr");
        prop.setSupportDefaultTranslations(true);
        prop.setSentenceSegmentingEnabled(false);
        IProject project = new NotLoadedProject() {
            @Override
            public ProjectProperties getProjectProperties() {
                return prop;
            }

            @Override
            public List<SourceTextEntry> getAllEntries() {
                List<SourceTextEntry> ste = new ArrayList<>();
                ste.add(new SourceTextEntry(new EntryKey("source.txt", "XXX", null, "", "", null),
                        1, null, null, new ArrayList<>()));
                return ste;
            }

            @Override
            public ITokenizer getSourceTokenizer() {
                return new LuceneEnglishTokenizer();
            };

            @Override
            public ITokenizer getTargetTokenizer() {
                return new DefaultTokenizer();
            }

            @Override
            public Map<Language, ProjectTMX> getOtherTargetLanguageTMs() {
                return Collections.emptyMap();
           }

            @Override
            public Map<String, ExternalTMX> getTransMemories() {
                Map<String, ExternalTMX> transMemories = new TreeMap<>();
                try {
                    ExternalTMX newTMX = ExternalTMFactory.load(file);
                    // newTMX will have two entries:
                    // - #0:
                    // source XXX
                    // translation XXX  lang = en-US
                    // - #1
                    // source XXX
                    // translation YYY  lang = sr
                    ITMXEntry en = newTMX.getEntries().get(0);
                    assertEquals("XXX", en.getSourceText());
                    assertEquals("XXX", en.getTranslationText());
                    en = newTMX.getEntries().get(1);
                    assertEquals("XXX", en.getSourceText());
                    assertEquals("YYY", en.getTranslationText());
                    transMemories.put(file.getPath(), newTMX);
                } catch (Exception ignored) {
                }
                return Collections.unmodifiableMap(transMemories);
            }
        };
        Core.setProject(project);
        Core.setSegmenter(new Segmenter(new SRX()));
        IStopped iStopped = () -> false;
        FindMatches finder = new FindMatches(project, OConsts.MAX_NEAR_STRINGS, true, false);
        List<NearString> result = finder.search("XXX", true, true, iStopped);
        // Without the fix, the result has two entries, but it should one.
        assertEquals(1, result.size());
        assertEquals("YYY", result.get(0).translation);
        FileUtils.deleteDirectory(tmpDir.toFile());
        assertFalse(tmpDir.toFile().exists());
    }
}