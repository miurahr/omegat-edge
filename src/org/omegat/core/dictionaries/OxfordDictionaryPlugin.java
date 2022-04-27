/**************************************************************************
Oxford dictionary API plugin for OmegaT CAT tool(http://www.omegat.org/)

 Copyright (C) 2020,2021 Hiroshi Miura

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

import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.core.events.IProjectEventListener;
import org.omegat.gui.preferences.PreferencesControllers;
import org.omegat.gui.preferences.view.OxfordPreferencesController;
import org.omegat.util.Language;
import org.omegat.util.Log;


public final class OxfordDictionaryPlugin {

    private static OnlineDictionaryApplicationEventListener listener = new OnlineDictionaryApplicationEventListener();
    private static OxfordDriver oxfordDriver;

    private OxfordDictionaryPlugin() { }

    /**
     * load plugin.
     */
    public static void loadPlugins() {
        CoreEvents.registerApplicationEventListener(listener);
        PreferencesControllers.addSupplier(OxfordPreferencesController::new);
    }

    /**
     * unload plugin.
     */
    public static void unloadPlugins() {
        CoreEvents.unregisterApplicationEventListener(listener);
        listener = null;
    }

    public static class OxfordDictionaryProjectEventListener implements IProjectEventListener {

        public OxfordDictionaryProjectEventListener() {
        }

        @Override
        public void onProjectChanged(final PROJECT_CHANGE_TYPE projectChangeType) {
            if (projectChangeType.equals(PROJECT_CHANGE_TYPE.LOAD)) {
                if (OxfordPreferencesController.isEnabled()) {
                    Language source = Core.getProject().getProjectProperties().getSourceLanguage();
                    Language target = Core.getProject().getProjectProperties().getTargetLanguage();
                    oxfordDriver = new OxfordDriver(source, target);
                    Core.getDictionaries().addDictionary(oxfordDriver);
                    Log.log("Oxford dictionary API plugin activated.");
                } else {
                    Log.log("Oxford dictionary API plugin disabled.");
                }
            } else if (projectChangeType.equals(PROJECT_CHANGE_TYPE.CLOSE)) {
                if (oxfordDriver != null) {
                    Core.getDictionaries().removeDictionary(oxfordDriver);
                    oxfordDriver = null;
                    Log.log("Oxford dictionary API plugin deactivated.");
                }
            }
        }

    }

    static class OnlineDictionaryApplicationEventListener implements IApplicationEventListener {

        @Override
        public void onApplicationStartup() {
            CoreEvents.registerProjectChangeListener(new OxfordDictionaryProjectEventListener());
        }

        @Override
        public void onApplicationShutdown() {
        }


    }
}
