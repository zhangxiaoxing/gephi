/*
Copyright 2008-2010 Gephi
Authors : Eduardo Ramos <eduramiba@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.datalab.plugin.manipulators.general.ui;

import java.awt.Color;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.datalab.api.DataTablesController;
import org.gephi.datalab.api.SearchReplaceController;
import org.gephi.datalab.api.SearchReplaceController.SearchOptions;
import org.gephi.datalab.api.SearchReplaceController.SearchResult;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.Node;
import org.gephi.utils.HTMLEscape;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Special UI for SearchReplace GeneralActionsManipulator
 * @author Eduardo Ramos <eduramiba@gmail.com>
 */
@ServiceProvider(service = SearchReplaceUI.class)
public final class SearchReplaceUI extends javax.swing.JPanel {

    public enum Mode {

        NODES_TABLE,
        EDGES_TABLE
    }
    private static final Color invalidRegexColor = new Color(254, 150, 150);
    private Mode mode = Mode.NODES_TABLE;
    private SearchReplaceController searchReplaceController;
    private DataTablesController dataTablesController;
    private SearchOptions searchOptions;
    private SearchResult searchResult = null;
    private Pattern regexPattern;
    private boolean active = false;

    /** Creates new form SearchReplaceUI */
    public SearchReplaceUI() {
        initComponents();

        searchReplaceController = Lookup.getDefault().lookup(SearchReplaceController.class);
        dataTablesController = Lookup.getDefault().lookup(DataTablesController.class);
        createSearchOptions();
        refreshSearchOptions();

        searchText.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                refreshSearchOptions();
            }

            public void removeUpdate(DocumentEvent e) {
                refreshSearchOptions();
            }

            public void changedUpdate(DocumentEvent e) {
                refreshSearchOptions();
            }
        });
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        createSearchOptions();
        resultText.setText("");
        refreshSearchOptions();
    }

    public void refreshSearchOptions() {
        refreshRegexPattern();
        searchOptions.setOnlyMatchWholeAttributeValue(matchWholeValueCheckBox.isSelected());
        searchOptions.setUseRegexReplaceMode(regexReplaceCheckBox.isEnabled() && regexReplaceCheckBox.isSelected());
        refreshControls();
    }

    private void createSearchOptions() {
        boolean onlyVisibleElements = Lookup.getDefault().lookup(DataTablesController.class).isShowOnlyVisible();
        searchResult = null;
        if (mode == Mode.NODES_TABLE) {
            Node[] nodes;
            if (onlyVisibleElements) {
                //Search on visible nodes:
                nodes = Lookup.getDefault().lookup(GraphController.class).getModel().getHierarchicalGraphVisible().getNodesTree().toArray();
            } else {
                nodes = new Node[0];//Search on all nodes
            }
            searchOptions = new SearchOptions(nodes, null);
        } else {
            Edge[] edges;
            if (onlyVisibleElements) {
                //Search on visible edges:
                edges = Lookup.getDefault().lookup(GraphController.class).getModel().getHierarchicalGraphVisible().getEdges().toArray();
            } else {
                edges = new Edge[0];//Search on all edges
            }
            searchOptions = new SearchOptions(edges, null);
        }
    }

    private void refreshRegexPattern() {
        try {
            String text = searchText.getText();
            if (normalSearchModeRadioButton.isSelected()) {
                text = Pattern.quote(text);//Normal search, make regex for literal string
                regexReplaceCheckBox.setEnabled(false);
                regexReplaceCheckBox.setSelected(false);
            } else {
                regexReplaceCheckBox.setEnabled(true);
            }

            if (!caseSensitiveCheckBox.isSelected()) {
                regexPattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
            } else {
                regexPattern = Pattern.compile(text);
            }
            searchOptions.setRegexPattern(regexPattern);
            searchText.setBackground(Color.WHITE);
        } catch (PatternSyntaxException ex) {
            searchText.setBackground(invalidRegexColor);
            regexPattern = null;
        }
    }

    private void refreshControls() {
        if (searchResult == null) {
            replaceButton.setEnabled(false);
            replaceAllButton.setEnabled(false);
        } else {
            replaceButton.setEnabled(searchReplaceController.canReplace(searchResult));
            replaceAllButton.setEnabled(true);
        }

        if (regexPattern == null) {
            findNextButton.setEnabled(false);
            replaceButton.setEnabled(false);
            replaceAllButton.setEnabled(false);
        } else {
            findNextButton.setEnabled(true);
        }
    }

    private void showSearchResult() {
        if (searchResult != null) {
            Object value;
            if (mode == Mode.NODES_TABLE) {
                Node node = searchResult.getFoundNode();
                dataTablesController.setNodeTableSelection(new Node[]{node});
                if (!dataTablesController.isNodeTableMode()) {
                    dataTablesController.selectNodesTable();
                }
                value = node.getNodeData().getAttributes().getValue(searchResult.getFoundColumnIndex());
            } else {
                Edge edge = searchResult.getFoundEdge();
                dataTablesController.setEdgeTableSelection(new Edge[]{edge});
                if (!dataTablesController.isEdgeTableMode()) {
                    dataTablesController.selectEdgesTable();
                }
                value = edge.getEdgeData().getAttributes().getValue(searchResult.getFoundColumnIndex());
            }

            String columnName;
            if (mode == Mode.NODES_TABLE) {
                columnName = Lookup.getDefault().lookup(AttributeController.class).getModel().getNodeTable().getColumn(searchResult.getFoundColumnIndex()).getTitle();
            } else {
                columnName = Lookup.getDefault().lookup(AttributeController.class).getModel().getEdgeTable().getColumn(searchResult.getFoundColumnIndex()).getTitle();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append(NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.column", HTMLEscape.stringToHTMLString(columnName)));
            sb.append("<br>");
            if (value != null) {
                String text = value.toString();
                sb.append(HTMLEscape.stringToHTMLString(text.substring(0, searchResult.getStart())));
                sb.append("<font color='blue'>");
                sb.append(HTMLEscape.stringToHTMLString(text.substring(searchResult.getStart(), searchResult.getEnd())));
                sb.append("</font>");
                sb.append(HTMLEscape.stringToHTMLString(text.substring(searchResult.getEnd())));
            } else {
                sb.append("<font color='blue'>null</font>");
            }
            sb.append("</html>");
            resultText.setText(sb.toString());
        } else {
            JOptionPane.showMessageDialog(null, NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.not.found", searchText.getText()));
            resultText.setText("");
        }
    }

    private void showRegexReplaceError() {
        JOptionPane.showMessageDialog(null, NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.regexReplacementError"), NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.dialog.title.error"), JOptionPane.ERROR_MESSAGE);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        searchModeButtonGroup = new javax.swing.ButtonGroup();
        searchLabel = new javax.swing.JLabel();
        replaceLabel = new javax.swing.JLabel();
        matchWholeValueCheckBox = new javax.swing.JCheckBox();
        normalSearchModeRadioButton = new javax.swing.JRadioButton();
        regexSearchModeRadioButton = new javax.swing.JRadioButton();
        caseSensitiveCheckBox = new javax.swing.JCheckBox();
        findNextButton = new javax.swing.JButton();
        replaceButton = new javax.swing.JButton();
        replaceAllButton = new javax.swing.JButton();
        searchText = new javax.swing.JTextField();
        replaceText = new javax.swing.JTextField();
        scroll = new javax.swing.JScrollPane();
        resultText = new javax.swing.JTextPane();
        resultLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        regexReplaceCheckBox = new javax.swing.JCheckBox();

        searchLabel.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.searchLabel.text")); // NOI18N

        replaceLabel.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.replaceLabel.text")); // NOI18N

        matchWholeValueCheckBox.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.matchWholeValueCheckBox.text")); // NOI18N
        matchWholeValueCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                matchWholeValueCheckBoxItemStateChanged(evt);
            }
        });

        searchModeButtonGroup.add(normalSearchModeRadioButton);
        normalSearchModeRadioButton.setSelected(true);
        normalSearchModeRadioButton.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.normalSearchModeRadioButton.text")); // NOI18N
        normalSearchModeRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                normalSearchModeRadioButtonItemStateChanged(evt);
            }
        });

        searchModeButtonGroup.add(regexSearchModeRadioButton);
        regexSearchModeRadioButton.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.regexSearchModeRadioButton.text")); // NOI18N
        regexSearchModeRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                regexSearchModeRadioButtonItemStateChanged(evt);
            }
        });

        caseSensitiveCheckBox.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.caseSensitiveCheckBox.text")); // NOI18N
        caseSensitiveCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                caseSensitiveCheckBoxItemStateChanged(evt);
            }
        });

        findNextButton.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.findNextButton.text")); // NOI18N
        findNextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findNextButtonActionPerformed(evt);
            }
        });

        replaceButton.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.replaceButton.text")); // NOI18N
        replaceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceButtonActionPerformed(evt);
            }
        });

        replaceAllButton.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.replaceAllButton.text")); // NOI18N
        replaceAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAllButtonActionPerformed(evt);
            }
        });

        searchText.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.searchText.text")); // NOI18N

        replaceText.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.replaceText.text")); // NOI18N

        resultText.setContentType(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.resultText.contentType")); // NOI18N
        resultText.setEditable(false);
        scroll.setViewportView(resultText);

        resultLabel.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.resultLabel.text")); // NOI18N

        regexReplaceCheckBox.setText(org.openide.util.NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.regexReplaceCheckBox.text")); // NOI18N
        regexReplaceCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                regexReplaceCheckBoxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(matchWholeValueCheckBox)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(normalSearchModeRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(regexSearchModeRadioButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(caseSensitiveCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(regexReplaceCheckBox))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(searchLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(replaceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(searchText, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                                    .addComponent(replaceText, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE))
                                .addGap(41, 41, 41)))
                        .addGap(0, 0, 0)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(replaceAllButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(replaceButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(findNextButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
                    .addComponent(resultLabel))
                .addContainerGap())
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(findNextButton)
                        .addGap(35, 35, 35)
                        .addComponent(replaceButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(replaceAllButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(searchText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(searchLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(replaceText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(replaceLabel))
                        .addGap(18, 18, 18)
                        .addComponent(matchWholeValueCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(normalSearchModeRadioButton)
                            .addComponent(regexSearchModeRadioButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(caseSensitiveCheckBox)
                            .addComponent(regexReplaceCheckBox))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 1, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(resultLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void normalSearchModeRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_normalSearchModeRadioButtonItemStateChanged
        refreshSearchOptions();
    }//GEN-LAST:event_normalSearchModeRadioButtonItemStateChanged

    private void regexSearchModeRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_regexSearchModeRadioButtonItemStateChanged
        refreshSearchOptions();
    }//GEN-LAST:event_regexSearchModeRadioButtonItemStateChanged

    private void matchWholeValueCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_matchWholeValueCheckBoxItemStateChanged
        refreshSearchOptions();
    }//GEN-LAST:event_matchWholeValueCheckBoxItemStateChanged

    private void caseSensitiveCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_caseSensitiveCheckBoxItemStateChanged
        refreshSearchOptions();
    }//GEN-LAST:event_caseSensitiveCheckBoxItemStateChanged

    private void findNextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findNextButtonActionPerformed
        searchResult = searchReplaceController.findNext(searchOptions);
        refreshSearchOptions();
        showSearchResult();
    }//GEN-LAST:event_findNextButtonActionPerformed

    private void replaceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceButtonActionPerformed
        try {
            searchResult = searchReplaceController.replace(searchResult, replaceText.getText());
            refreshSearchOptions();
            dataTablesController.refreshCurrentTable();
            showSearchResult();
        } catch (Exception ex) {
            showRegexReplaceError();
        }
    }//GEN-LAST:event_replaceButtonActionPerformed

    private void replaceAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllButtonActionPerformed
        try {
            int replacementsCount = searchReplaceController.replaceAll(searchOptions, replaceText.getText());
            searchResult = null;
            refreshSearchOptions();
            dataTablesController.refreshCurrentTable();
            JOptionPane.showMessageDialog(null, NbBundle.getMessage(SearchReplaceUI.class, "SearchReplaceUI.replacements.count.message", replacementsCount));
            resultText.setText("");
        } catch (Exception ex) {
            showRegexReplaceError();
        }
    }//GEN-LAST:event_replaceAllButtonActionPerformed

    private void regexReplaceCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_regexReplaceCheckBoxItemStateChanged
        refreshSearchOptions();
    }//GEN-LAST:event_regexReplaceCheckBoxItemStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox caseSensitiveCheckBox;
    private javax.swing.JButton findNextButton;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JCheckBox matchWholeValueCheckBox;
    private javax.swing.JRadioButton normalSearchModeRadioButton;
    private javax.swing.JCheckBox regexReplaceCheckBox;
    private javax.swing.JRadioButton regexSearchModeRadioButton;
    private javax.swing.JButton replaceAllButton;
    private javax.swing.JButton replaceButton;
    private javax.swing.JLabel replaceLabel;
    private javax.swing.JTextField replaceText;
    private javax.swing.JLabel resultLabel;
    private javax.swing.JTextPane resultText;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JLabel searchLabel;
    private javax.swing.ButtonGroup searchModeButtonGroup;
    private javax.swing.JTextField searchText;
    // End of variables declaration//GEN-END:variables
}