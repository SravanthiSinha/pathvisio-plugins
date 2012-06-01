package org.tno.idmapper;

import org.bridgedb.IDMapperException;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * Small plugin that allows you to specify the
 * gene and metabolite databases to be loaded via
 * the java system properties (-D option).
 * @author thomas
 */
public class LoadIDMapperPlugin implements Plugin {
	public void done() {
	}
	
	public void init(PvDesktop desktop) {
		//Find out if any options were specified
		String geneLoc = System.getProperty("javaws.org.tno.gdb");
		String metLoc = System.getProperty("javaws.org.tno.mdb");
		
		//Should we override the user preferences?
		boolean override = "true".equals(System.getProperty("javaws.org.tno.dboverride"));
		
		Logger.log.info(getClass().getName() + ", geneLoc: " + geneLoc);
		Logger.log.info(getClass().getName() + ", metLoc: " + metLoc);
		Logger.log.info(getClass().getName() + ", override: " + override);
		
		//Connect to the databases
		String genePref = PreferenceManager.getCurrent().get(GlobalPreference.DB_CONNECTSTRING_GDB);
		if(geneLoc != null && (override || GlobalPreference.DB_CONNECTSTRING_GDB.getDefault().equals(genePref))) {
			try {
				desktop.getSwingEngine().getGdbManager().setGeneDb(geneLoc);
				//Do not overwrite preference
				PreferenceManager.getCurrent().set(GlobalPreference.DB_CONNECTSTRING_METADB, genePref);
			} catch(IDMapperException e) {
				Logger.log.error("Unable to load gene database", e);
			}
		}
		
		String metPref = PreferenceManager.getCurrent().get(GlobalPreference.DB_CONNECTSTRING_METADB);
		if(metLoc != null && (override || GlobalPreference.DB_CONNECTSTRING_METADB.getDefault().equals(metPref))) {
			try {
				desktop.getSwingEngine().getGdbManager().setMetaboliteDb(metLoc);
				//Do not overwrite preference
				PreferenceManager.getCurrent().set(GlobalPreference.DB_CONNECTSTRING_METADB, metPref);
			} catch(IDMapperException e) {
				Logger.log.error("Unable to load metabolite database", e);
			}
		}
	}
}
