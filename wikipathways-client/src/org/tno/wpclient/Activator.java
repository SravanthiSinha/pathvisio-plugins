package org.tno.wpclient;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

public class Activator implements BundleActivator {
	public void start(BundleContext context) throws Exception {
		WikiPathwaysClientPlugin plugin = new WikiPathwaysClientPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);
	}

	public void stop(BundleContext context) throws Exception {}
}
