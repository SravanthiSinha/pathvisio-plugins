package org.tno.idmapper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

public class Activator implements BundleActivator {
	public void start(BundleContext context) throws Exception {
		LoadIDMapperPlugin plugin = new LoadIDMapperPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
