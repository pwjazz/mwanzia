package org.mwanzia;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of Mwanzia.
 * 
 * @author percy.wegmann
 * 
 */
public class DefaultMwanzia extends Mwanzia {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMwanzia.class);

	private static String s_javascript;

	private Map<String, Application> applications = new HashMap<String, Application>();

	public DefaultMwanzia(Map<String, String> config) throws Exception {
		super(config);
		String applicationName = config.get("application");
		// TODO: add support for multiple applications
		Application application = (Application) this.getClass().getClassLoader().loadClass(applicationName)
					.newInstance();
		this.applications.put(application.getName(), application);
		buildJavaScript();
	}

	public String getJavaScript(String baseUrl) throws Exception {
		buildJavaScript();
		StringBuilder js = new StringBuilder(s_javascript);
		for (Map.Entry<String, Application> entry : applications.entrySet()) {
			js.append("\n");
			js.append(entry.getValue().javaScriptInstance(baseUrl));
		}
		return js.toString();
	}

	public String call(String applicationName, String targetClass, String methodName, String callString)
			throws Exception {
		try {
		    return applications.get(applicationName).invoke(targetClass, methodName, callString);
		} catch (Exception e) {
            LOGGER.error(String.format("Unable to make call to application %1s: %2$s", applicationName, e.getMessage()),
                    e);
            throw e;
		}
	}

	private void buildJavaScript() throws ServletException {
		LOGGER.info("Building Mwanzia JavaScript");
		try {
			if (s_javascript == null
					|| "dev".equalsIgnoreCase(System.getProperty("mwanzia.mode"))) {
				StringBuilder javascript = new StringBuilder();
				for (Application application : applications.values()) {
					javascript.append(application.coreJavaScript());
					javascript.append("\n\n");
				}
				s_javascript = javascript.toString();
			}
		} catch (Exception e) {
			throw new ServletException(e.getMessage(), e);
		}
	}
}
