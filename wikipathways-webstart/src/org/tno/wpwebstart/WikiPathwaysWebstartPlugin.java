package org.tno.wpwebstart;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.tno.wpwebstart.WikiPathwaysHandler.Parameter;

public class WikiPathwaysWebstartPlugin implements Plugin {
	@Override
	public void init(final PvDesktop desktop) {
		final WikiPathwaysHandler wpHandler = new WikiPathwaysHandler(desktop, System.getProperties());
		
		if(wpHandler.getParameter(Parameter.pwId) == null) return;
		
		//Add button to save to wikipathways
		Action saveAction = new WebSaveAction(wpHandler);
		JButton saveButton = new JButton(saveAction);
		//saveButton.setText("");
		desktop.getSwingEngine().getApplicationPanel().getToolBar().add(saveButton, 0);
		
		try {
			wpHandler.loadWithProgress();
		} catch (Throwable e) {
			Logger.log.error("Error while loading info from WikiPathways server", e);
			JOptionPane.showMessageDialog(desktop.getSwingEngine().getApplicationPanel(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}				
	}
	
	@Override
	public void done() { }
	
}
