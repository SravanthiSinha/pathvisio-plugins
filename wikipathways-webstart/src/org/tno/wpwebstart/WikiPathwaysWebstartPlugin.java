package org.tno.wpwebstart;

import java.util.Properties;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.tno.wpwebstart.WikiPathwaysHandler.Parameter;

public class WikiPathwaysWebstartPlugin implements Plugin {
	@Override
	public void init(final PvDesktop desktop) {
		//Fix for running from jnlp, look for properties with "jnlp." prefix
		Properties props = System.getProperties();
		for(Parameter p : Parameter.values()) {
			if(props.get(p.name()) == null && System.getProperty("javaws." + p.name()) != null) {
				props.put(p.name(), System.getProperty("javaws." + p.name()));
			}
		}
		Logger.log.info("Modified system properties: " + props.toString());
		final WikiPathwaysHandler wpHandler = new WikiPathwaysHandler(desktop, props);
		
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
