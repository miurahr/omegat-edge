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

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


@SuppressWarnings({"visibilitymodifier", "serial"})
public class OxfordOptionsPanel extends JPanel {
    JTextField appIdField;
    JTextField appKeyField;
    JCheckBox enableOption;
    JRadioButton queryMonolingual;
    JRadioButton queryBilingual;
    JRadioButton queryBoth;
    ButtonGroup buttonGroup;

    public OxfordOptionsPanel() {
        initGui();
        enableOption.addActionListener(e -> {
            appIdField.setEnabled(enableOption.isSelected());
            appKeyField.setEnabled(enableOption.isSelected());
        });
        appIdField.setInputVerifier(new OxfordInputVerifier());
        appKeyField.setInputVerifier(new OxfordInputVerifier());
    }

    class OxfordInputVerifier extends javax.swing.InputVerifier implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            // shouldYieldFocus(field);
            field.selectAll();
        }

        @Override
        public boolean verify(final JComponent input) {
            if (input == appIdField) {
                String str = appIdField.getText();
                appIdField.setText(str.replaceAll("\\s|\\t", ""));
                return true;
            } else if (input == appKeyField) {
                String str = appKeyField.getText();
                appKeyField.setText(str.replaceAll("\\s|\\t", ""));
                return true;
            } else {
                return true;
            }
        }
    }

    private void initGui() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JPanel cbPanel = new JPanel();
        JPanel idPanel = new JPanel();
        JPanel keyPanel = new JPanel();
        JPanel queryPanel = new JPanel();
        enableOption = new JCheckBox("Enable Oxford Dictionaries");
        queryMonolingual = new JRadioButton("Query monolingual dictionary");
        queryBilingual = new JRadioButton("Query bilingual dictionary");
        queryBoth = new JRadioButton("Query both mono/bi-ligual dictionary");
        buttonGroup = new ButtonGroup();
        buttonGroup.add(queryMonolingual);
        buttonGroup.add(queryBilingual);
        buttonGroup.add(queryBoth);
        appIdField = new JTextField();
        appKeyField = new JTextField();
        appIdField.setPreferredSize(new Dimension(300, 30));
        appKeyField.setPreferredSize(new Dimension(300, 30));
        appIdField.setHorizontalAlignment(JTextField.LEFT);
        appKeyField.setHorizontalAlignment(JTextField.LEFT);
        JLabel appIdLabel = new JLabel();
        JLabel appKeyLabel = new JLabel();
        appIdLabel.setText("OD API App ID : ");
        appKeyLabel.setText("OD API AppKey : ");
        cbPanel.add(enableOption);
        cbPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        idPanel.add(appIdLabel);
        idPanel.add(appIdField);
        idPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        keyPanel.add(appKeyLabel);
        keyPanel.add(appKeyField);
        keyPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.PAGE_AXIS));
        queryPanel.add(queryMonolingual);
        queryPanel.add(queryBilingual);
        queryPanel.add(queryBoth);
        queryPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        add(cbPanel);
        add(idPanel);
        add(keyPanel);
        add(queryPanel);
    }
}
