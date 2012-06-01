package org.tno.template;

import java.io.File;

import javax.swing.JTabbedPane;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * PathViso Plugin that adds a panel in the side panel
 * to which pathway can be loaded. This can be used to
 * provide quick access to a set of template elements.
 */
public class TemplatePanelPlugin implements Plugin {
	@Override
	public void init(PvDesktop desktop) {
		Logger.log.info("Loading TemplatePanelPlugin");
		JTabbedPane tabPane = desktop.getSideBarTabbedPane();
		TemplatePanel tmplPanel = new TemplatePanel(desktop);
		tabPane.addTab("Template", tmplPanel);
		
		try {
			//If template is specified by property, load it
			String resProp = System.getProperty("org.tno.template.resource");
			String fileProp = System.getProperty("org.tno.template.file");
			//Else load last used template, if available
			String last = PreferenceManager.getCurrent().get(TemplatePreference.LAST_TEMPLATE);
			if(resProp != null) {
				Logger.log.info("Autoloading template from resource " + resProp);
				Pathway p = new Pathway();
				p.readFromXml(getClass().getResourceAsStream(resProp), true);
				tmplPanel.setInput(p);
			} else if(fileProp != null) {
				Logger.log.info("Autoloading template from file " + fileProp);
				Pathway p = new Pathway();
				p.readFromXml(new File(fileProp), true);
				tmplPanel.setInput(p);
			} else if(last != null && new File(last).canRead()){
				Logger.log.info("Autoloading template from last used " + last);
				Pathway p = new Pathway();
				p.readFromXml(new File(last), true);
				tmplPanel.setInput(p);
			} else {
				Logger.log.info("No templates autoloaded.");
			}
		} catch(Exception e) {
			Logger.log.warn("Unable to autoload templates", e);
		}
	}
	
	@Override
	public void done() {
		
	}
	
	enum TemplatePreference implements Preference {
		LAST_TEMPLATE;
		
		public String getDefault() {
			return null;
		}
	}
}
