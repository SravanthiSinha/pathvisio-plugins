package org.tno.wpwebstart;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

public class Activator implements BundleActivator {
	public void start(BundleContext context) throws Exception {
		WikiPathwaysWebstartPlugin plugin = new WikiPathwaysWebstartPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
