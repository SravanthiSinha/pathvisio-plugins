package org.tno.template;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.Resources;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.core.view.SelectionBox.SelectionEvent;
import org.pathvisio.core.view.SelectionBox.SelectionListener;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gui.view.VPathwaySwing;
import org.tno.template.TemplatePanelPlugin.TemplatePreference;

public class TemplatePanel extends JPanel {
	JScrollPane pathwayPanel;
	JPanel buttonPanel;
	JButton loadButton;

	VPathway vPathway;
	PvDesktop pvDesktop;
	
	public TemplatePanel(PvDesktop pvDesktop) {
		this.pvDesktop = pvDesktop;
		setLayout(new BorderLayout());
		init();
	}

	private void init() {
		buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pathwayPanel = new JScrollPane();
		
		loadButton = new JButton("", new ImageIcon(Resources
				.getResourceURL("open.gif")));
		loadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseInput();
			}
		});
		buttonPanel.add(loadButton);

		add(buttonPanel, BorderLayout.SOUTH);
		add(pathwayPanel, BorderLayout.CENTER);
		
		VPathwaySwing vps = new VPathwaySwing(pathwayPanel);
		vPathway = vps.createVPathway();
		
		//Disable zoom when scrolling
		vps.removeMouseWheelListener(vps);
		
		//Add template elements upon clicking pathway
		pvDesktop.getSwingEngine().getEngine().addApplicationEventListener(new ApplicationEventListener() {
			public void applicationEvent(ApplicationEvent e) {
				if(e.getType() == ApplicationEvent.Type.VPATHWAY_CREATED) {
					final VPathway target = (VPathway)e.getSource();
					((VPathwaySwing)target.getWrapper()).addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent e) {
							//If anything is selected on the template pathway
							//paste it and deselect
							if(vPathway.getSelectedGraphics().size() > 0) {
								Point p = new Point(
									(int)(e.getPoint().x / target.getZoomFactor()),
									(int)(e.getPoint().y / target.getZoomFactor())
								);
								target.positionPasteFromClipboard(p);
								vPathway.clearSelection();
							}
						}
					});
				}
			}
		});
	}

	public void setInput(Pathway p) {
		vPathway.fromModel(p);
		vPathway.setEditMode(false);
		vPathway.redraw();
		
		//Add a selection listener that automatically copies
		//selected elements
		vPathway.addSelectionListener(new SelectionListener() {
			public void selectionEvent(SelectionEvent e) {
				vPathway.copyToClipboard();
			}
		});
	}

	private void chooseInput() {
		JFileChooser jfc = new JFileChooser();
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.setDialogType(JFileChooser.OPEN_DIALOG);
		jfc.setCurrentDirectory(PreferenceManager.getCurrent().getFile(
				GlobalPreference.DIR_LAST_USED_OPEN));

		jfc.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				String ext = f.toString().substring(f.toString().length() - 4);
				if (ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("gpml")) {
					return true;
				}
				return false;
			}

			public String getDescription() {
				return "GPML files (*.gpml, *.xml)";
			}
		});

		int status = jfc.showDialog(this, "Open template pathway");
		if (status == JFileChooser.APPROVE_OPTION) {
			File f = jfc.getSelectedFile();
			
			Pathway p = new Pathway();
			try {
				p.readFromXml(f, true);
				setInput(p);
				
				//Store the last used template
				PreferenceManager.getCurrent().setFile(TemplatePreference.LAST_TEMPLATE, f);
			} catch (ConverterException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(),
						"Error loading pathway", JOptionPane.ERROR_MESSAGE);
				Logger.log.error("Error loading template pathway", e);
			}
		}
	}
}
