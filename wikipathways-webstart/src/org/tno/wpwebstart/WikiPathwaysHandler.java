package org.tno.wpwebstart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.apache.xmlrpc.client.XmlRpcHttpTransport;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.util.HttpUtil;
import org.bridgedb.IDMapperException;
import org.pathvisio.core.Engine;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.GpmlFormat;
import org.pathvisio.core.util.FileUtils;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gui.ProgressDialog;
import org.xml.sax.SAXException;

public class WikiPathwaysHandler {
	public static enum Parameter {
		pwId, revision, rpcUrl, cookies, pwUrl, siteName, gdb_server
	}

	private Properties parameters;
	private PvDesktop desktop;

	public WikiPathwaysHandler(PvDesktop pvDesktop, Properties startParameters) {
		desktop = pvDesktop;
		parameters = startParameters;
	}

	public String getParameter(Parameter name) {
		Object v = parameters.get(name.name());
		return v == null ? null : ("" + v);
	}

	public void setParameter(Parameter name, String value) {
		parameters.put(name.name(), value);
	}

	private Map<String, String> parseCookies(String cookieString) {
		Map<String, String> cookies = new HashMap<String, String>();
		for(String c : cookieString.split(";")) {
			String[] keyvalue = c.trim().split("=", 2);
			cookies.put(keyvalue[0], keyvalue[1]);
		}
		return cookies;
	}

	public PvDesktop getDesktop() {
		return desktop;
	}

	public boolean isPathwaySpecified() {
		return getParameter(Parameter.pwId) != null;
	}
	
	public void loadWithProgress() throws Exception {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getDesktop().getSwingEngine().getApplicationPanel()),
				"", pk, false, true);

		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
			protected Boolean doInBackground() throws Exception {
				pk.setTaskName("Opening pathway");
				try {
					if(isPathwaySpecified()) load();
				} catch(Exception e) {
					throw e;
				} finally {
					pk.finished();
				}
				return true;
			}
		};

		sw.execute();
		d.setVisible(true);
		sw.get();
	}
	
	public void saveWithProgress() throws Exception {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getDesktop().getSwingEngine().getApplicationPanel()),
				"", pk, false, true);

		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
			protected Boolean doInBackground() throws Exception {
				pk.setTaskName("Saving pathway");
				try {
					if(isPathwaySpecified()) save("");
				} catch(Exception e) {
					throw e;
				} finally {
					pk.finished();
				}
				return true;
			}
		};

		sw.execute();
		d.setVisible(true);
		sw.get();
	}
	
	public void load() throws ConverterException, ClassNotFoundException, IDMapperException, IOException {
		//Load the pathway
		Engine engine = desktop.getSwingEngine().getEngine();
		engine.setWrapper(desktop.getSwingEngine().createWrapper());
		
		File f = File.createTempFile(getParameter(Parameter.pwId) + " @ " + getParameter(Parameter.siteName) + " ", "." + Engine.PATHWAY_FILE_EXTENSION);
		
		//Load from cache if available
		boolean cacheLoaded = false;
		if(f.exists()) {
			try {
				engine.openPathway(f);
				cacheLoaded = true;
			} catch(Exception e) {
				Logger.log.warn("Unable to load cached pathway", e);
			}
		}
		if(!cacheLoaded) {
			FileUtils.downloadFile(new URL(getParameter(Parameter.pwUrl)), f);
			engine.openPathway(f);
		}

		String species = desktop.getSwingEngine().getEngine().getActivePathway().getMappInfo().getOrganism();

		//Connect to id mapper
		String bridgeUrl = getParameter(Parameter.gdb_server);
		if(!bridgeUrl.endsWith("/")) bridgeUrl = bridgeUrl + "/";
		String geneUrl = "";
		if(bridgeUrl.startsWith("idmapper-bridgerest")) {
			Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
			geneUrl = bridgeUrl + URLEncoder.encode(species, "UTF-8");
		} else if(bridgeUrl.startsWith("idmapper-jdbc")) {
			Class.forName("org.apache.derby.jdbc.ClientDriver");
			Class.forName("org.bridgedb.rdb.IDMapperRdb");
			geneUrl = bridgeUrl +  species;
		}
		GdbManager gdbManager = desktop.getSwingEngine().getGdbManager();

		Logger.log.trace("Bridgedb connection string: " + geneUrl);
		if(!"".equals(geneUrl)) gdbManager.addMapper(geneUrl);

		//Also connect to the metabolite database if we're using derby
		if(bridgeUrl.startsWith("idmapper-jdbc")) {
			String metUrl = bridgeUrl + "metabolites";
			if(!"".equals(metUrl)) gdbManager.addMapper(metUrl);
		}
	}

	public void save(String description) throws MalformedURLException, ConverterException, XmlRpcException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(getParameter(Parameter.rpcUrl)));

		XmlRpcClient client = new XmlRpcClient();
		XmlRpcCookieTransportFactory ctf = new XmlRpcCookieTransportFactory(client);

		XmlRpcCookieHttpTransport ct = (XmlRpcCookieHttpTransport)ctf.getTransport();
		Map<String, String> cookies = parseCookies(getParameter(Parameter.cookies));
		for(String key : cookies.keySet()) {
			ct.addCookie(key, cookies.get(key));
		}

		client.setTransportFactory(ctf);
		client.setConfig(config);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GpmlFormat.writeToXml(desktop.getSwingEngine().getEngine().getActivePathway(), out, true);

		byte[] data = out.toByteArray();

		Object[] params = new Object[]{ getParameter(Parameter.pwId), description, data, Integer.parseInt(getParameter(Parameter.revision)) };
		Object response = client.execute("WikiPathways.updatePathway", params);
		//Update the revision in case we want to save again
		setParameter(Parameter.revision, (String)response);
		//Also save temporary file to prevent "want to save?" dialog upon closing PathVisio
		desktop.getSwingEngine().savePathway();
	}

	static class XmlRpcCookieTransportFactory implements XmlRpcTransportFactory {
		private final XmlRpcCookieHttpTransport transport;

		public XmlRpcCookieTransportFactory(XmlRpcClient pClient) {
			transport = new XmlRpcCookieHttpTransport(pClient);
		}

		public XmlRpcTransport getTransport() { return transport; }
	}

	/** Implementation of an HTTP transport that supports sending cookies with the
	 * HTTP header, based on the {@link java.net.HttpURLConnection} class.
	 */
	public static class XmlRpcCookieHttpTransport extends XmlRpcHttpTransport {
		private static final String USER_AGENT_MOD = USER_AGENT + " (Sun HTTP Transport, mod Thomas)";
		private static final String COOKIE_HEADER = "Cookie";
		private URLConnection conn;
		private Map<String, String> cookie;

		public XmlRpcCookieHttpTransport(XmlRpcClient pClient) {
			super(pClient, USER_AGENT_MOD);
			cookie = new HashMap<String, String>();
		}

		public void addCookie(String key, String value) {
			cookie.put(key, value);
		}

		protected void setCookies() {
			String cookieString = null;
			for(String key : cookie.keySet()) {
				cookieString = (cookieString == null ? "" : cookieString + "; ") + key + "=" + cookie.get(key);
			}
			if(cookieString != null) {
				conn.setRequestProperty(COOKIE_HEADER, cookieString);
			}
		}

		public Object sendRequest(XmlRpcRequest pRequest) throws XmlRpcException {
			XmlRpcHttpClientConfig config = (XmlRpcHttpClientConfig) pRequest.getConfig();
			try {
				conn = config.getServerURL().openConnection();
				conn.setUseCaches(false);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				setCookies();
			} catch (IOException e) {
				throw new XmlRpcException("Failed to create URLConnection: " + e.getMessage(), e);
			}
			return super.sendRequest(pRequest);
		}

		protected void setRequestHeader(String pHeader, String pValue) {
			conn.setRequestProperty(pHeader, pValue);

		}

		protected void close() throws XmlRpcClientException {
			if (conn instanceof HttpURLConnection) {
				((HttpURLConnection) conn).disconnect();
			}
		}

		protected boolean isResponseGzipCompressed(XmlRpcStreamRequestConfig pConfig) {
			return HttpUtil.isUsingGzipEncoding(conn.getHeaderField("Content-Encoding"));
		}

		protected InputStream getInputStream() throws XmlRpcException {
			try {
				return conn.getInputStream();
			} catch (IOException e) {
				throw new XmlRpcException("Failed to create input stream: " + e.getMessage(), e);
			}
		}

		protected void writeRequest(ReqWriter pWriter) throws IOException, XmlRpcException, SAXException {
			pWriter.write(conn.getOutputStream());
		}
	}
}
