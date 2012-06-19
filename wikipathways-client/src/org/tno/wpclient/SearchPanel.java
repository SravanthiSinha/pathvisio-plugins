package org.tno.wpclient;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.wikipathways.webservice.WSSearchResult;
import org.wikipathways.client.WikiPathwaysClient;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SearchPanel extends JPanel {
	WikiPathwaysClientPlugin plugin;
	JTextField searchField;
	JComboBox clientDropdown;
	JTable resultTable;

	public SearchPanel(final WikiPathwaysClientPlugin plugin) {
		this.plugin = plugin;

		setLayout(new BorderLayout());

		Action searchAction = new AbstractAction("Search") {
			public void actionPerformed(ActionEvent e) {
				try {
					search();
				} catch (Exception ex) {
					JOptionPane
							.showMessageDialog(SearchPanel.this,
									ex.getMessage(), "Error",
									JOptionPane.ERROR_MESSAGE);
					Logger.log.error("Error searching WikiPathways", ex);
				}
			}
		};

		// North contains panel with:
		// - search input
		// - search button
		// - dropdown box for choosing client
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FormLayout(
				"4dlu, fill:30dlu:grow, 4dlu, pref, 4dlu, pref, 4dlu", "pref"));
		CellConstraints cc = new CellConstraints();
		searchField = new JTextField();
		searchField.setAction(searchAction);

		topPanel.add(searchField, cc.xy(2, 1));

		JButton searchButton = new JButton(searchAction);
		topPanel.add(searchButton, cc.xy(4, 1));

		Vector<String> clients = new Vector<String>(plugin.getClients()
				.keySet());
		Collections.sort(clients);
		clientDropdown = new JComboBox(clients);
		clientDropdown.setSelectedIndex(0);
		clientDropdown.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				String strValue = shortClientName(value.toString());

				return super.getListCellRendererComponent(list, strValue,
						index, isSelected, cellHasFocus);

			}
		});
		topPanel.add(clientDropdown, cc.xy(6, 1));
		if(plugin.getClients().size() < 2) clientDropdown.setVisible(false);
		
		add(topPanel, BorderLayout.NORTH);

		// Center contains table model for results
		resultTable = new JTable();
		add(new JScrollPane(resultTable), BorderLayout.CENTER);
		searchField.requestDefaultFocus();

		resultTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					JTable target = (JTable) e.getSource();
					int row = target.getSelectedRow();
					SearchTableModel model = (SearchTableModel)target.getModel();

					File tmpDir = new File(plugin.getTmpDir(),
							shortClientName(model.clientName));
					tmpDir.mkdirs();
					try {
						plugin.openPathwayWithProgress(
								plugin.getClients().get(model.clientName),
								model.getValueAt(row, 0).toString(), 0,
								tmpDir);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(SearchPanel.this,
								ex.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
						Logger.log.error("Error", ex);
					}
				}
			}
		});
	}

	private String shortClientName(String clientName) {
		Pattern pattern = Pattern.compile("http://(.*?)/");
		Matcher matcher = pattern.matcher(clientName);
		if (matcher.find()) clientName = matcher.group(1);
		return clientName;
	}
	
	private void search() throws RemoteException, InterruptedException, ExecutionException {
		final String query = searchField.getText();
		String clientName = clientDropdown.getSelectedItem().toString();
		
		final WikiPathwaysClient client = plugin.getClients().get(clientName);

		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(this),
				"", pk, true, true);

		SwingWorker<WSSearchResult[], Void> sw = new SwingWorker<WSSearchResult[], Void>() {
			protected WSSearchResult[] doInBackground() throws Exception {
				pk.setTaskName("Searching");
				WSSearchResult[] results = null;
				try {
					System.out.println("HERE");
					results = client.findPathwaysByText(query);
					System.out.println("HERE");
				} catch(Exception e) {
					throw e;
				} finally {
					pk.finished();
				}
				return results;
			}
		};

		sw.execute();
		d.setVisible(true);
		resultTable.setModel(new SearchTableModel(sw.get(), clientName));
		resultTable.setRowSorter(new TableRowSorter(resultTable.getModel()));
	}

	private class SearchTableModel extends AbstractTableModel {
		WSSearchResult[] results;
		String[] columnNames = new String[] { "ID", "Name", "Species" };
		String clientName;
		
		public SearchTableModel(WSSearchResult[] results, String clientName) {
			this.clientName = clientName;
			this.results = results;
		}

		public int getColumnCount() {
			return 3;
		}

		public int getRowCount() {
			return results.length;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			WSSearchResult r = results[rowIndex];
			switch (columnIndex) {
			case 0:
				return r.getId();
			case 1:
				return r.getName();
			case 2:
				return r.getSpecies();
			}
			return "";
		}

		public String getColumnName(int column) {
			return columnNames[column];
		}
	}
}
