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
package org.omegat.gui.preferences.view;

import org.omegat.gui.preferences.BasePreferencesController;
import org.omegat.util.CredentialsManager;
import org.omegat.util.Preferences;

import java.awt.Component;

public class OxfordPreferencesController extends BasePreferencesController {

    private static final String OPTION_OXFORD_ENABLED = "dictionary_oxford_enabled";
    private static final String OPTION_OXFORD_APPID = "dictionary_oxford_appid";
    private static final String OPTION_OXFORD_APPKEY = "dictionary_oxford_appkey";
    private static final String OPTION_OXFORD_MONO = "dictionary_oxford_mono";
    private static final String OPTION_OXFORD_BILINGUAL = "dictionary_oxford_bilingual";
    private OxfordOptionsPanel panel;

    public static boolean isEnabled() {
        return Preferences.isPreferenceDefault(OPTION_OXFORD_ENABLED, false);
    }

    public static String getAppId() {
        return getCredential(OPTION_OXFORD_APPID);
    }

    public static String getAppKey() {
        return getCredential(OPTION_OXFORD_APPKEY);
    }

    public static boolean isBilingual() {
        return Preferences.isPreferenceDefault(OPTION_OXFORD_BILINGUAL, false);
    }

    public static boolean isMonolingual() {
        return Preferences.isPreferenceDefault(OPTION_OXFORD_MONO, true);
    }

    /**
     * Retrieve a credential with the given ID. First checks temporary system properties, then falls back to
     * the program's persistent preferences. Store a credential with
     * {@link #setCredential(String, String)}.
     *
     * @param id ID or key of the credential to retrieve
     * @return the credential value in plain text
     */
     protected static String getCredential(final String id) {
        String property = System.getProperty(id);
        if (property != null) {
            return property;
        }
        return CredentialsManager.getInstance().retrieve(id).orElse("");
    }

    /**
     * Store a credential. Credentials are stored in temporary system properties
     * and in the program's persistent preferences encoded in
     * Base64. Retrieve a credential with {@link #getCredential(String)}.
     *
     * @param id        ID or key of the credential to store
     * @param value     value of the credential to store
     */
     protected static void setCredential(final String id, final String value) {
        System.setProperty(id, value);
        CredentialsManager.getInstance().store(id, value);
    }

    private void setState() {
        panel.appIdField.setEnabled(panel.enableOption.isSelected());
        panel.appKeyField.setEnabled(panel.enableOption.isSelected());
        panel.queryMonolingual.setEnabled(panel.enableOption.isSelected());
        panel.queryBilingual.setEnabled(panel.enableOption.isSelected());
    }

    @Override
    protected void initFromPrefs() {
        panel.enableOption.setSelected(isEnabled());
        if (isMonolingual() && isBilingual()) {
            panel.queryBoth.setSelected(true);
        } else if (isBilingual()) {
            panel.queryBilingual.setSelected(true);
        } else {
            panel.queryMonolingual.setSelected(true);
        }
        panel.appIdField.setText(getCredential(OPTION_OXFORD_APPID));
        panel.appKeyField.setText(getCredential(OPTION_OXFORD_APPKEY));
        setState();
   }

    @Override
    public String toString() {
        return "Oxford Dictionaries API";
    }

    @Override
    public Component getGui() {
        if (panel == null) {
            initGui();
            initFromPrefs();
        }
        return panel;
    }

    @Override
    public void persist() {
        Preferences.setPreference(OPTION_OXFORD_ENABLED, panel.enableOption.isSelected());
        if (panel.queryBoth.isSelected()) {
            Preferences.setPreference(OPTION_OXFORD_MONO, true);
            Preferences.setPreference(OPTION_OXFORD_BILINGUAL, true);
        } else {
            Preferences.setPreference(OPTION_OXFORD_MONO, panel.queryMonolingual.isSelected());
            Preferences.setPreference(OPTION_OXFORD_BILINGUAL, panel.queryBilingual.isSelected());
        }
        setCredential(OPTION_OXFORD_APPID, panel.appIdField.getText());
        setCredential(OPTION_OXFORD_APPKEY, panel.appKeyField.getText());
        setRestartRequired(true);
    }

    @Override
    public void restoreDefaults() {
        panel.enableOption.setSelected(false);
        panel.appIdField.setEnabled(false);
        panel.appKeyField.setEnabled(false);
        panel.queryMonolingual.setSelected(true);
        setState();
    }

    private void initGui() {
        panel = new OxfordOptionsPanel();
    }
}
