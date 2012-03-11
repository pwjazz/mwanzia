package org.mwanzia;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Servlet responsible both for rendering the JavaScript for exporting
 * Applications and for processing inbound calls from the client tier. A GET
 * request to this servlet returns the JavaScript form of the Application while
 * a POST request is handled as a remote invocation.
 * </p>
 * 
 * @author percy
 * 
 */
public class MwanziaServlet extends HttpServlet {
	private static final long serialVersionUID = -3767422239799721120L;
	protected Mwanzia mwanzia;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		getJavaScript(req, resp, getServletContext());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		call(req, resp, getServletContext());
	}

	protected void getJavaScript(HttpServletRequest req,
			HttpServletResponse resp, ServletContext servletContext)
			throws ServletException {
		try {
			byte[] bytes = getJavaScript(req.getRequestURL().toString()).getBytes(Charset.forName("UTF-8"));
			resp.setContentType("text/javascript; charset=utf-8");
			resp.setContentLength(bytes.length);
			resp.getOutputStream().write(bytes);
		} catch (Exception e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	protected String getJavaScript(String baseUrl) throws Exception {
		return mwanzia.getJavaScript(baseUrl);
	}

	protected void call(HttpServletRequest req,
			HttpServletResponse resp, ServletContext servletContext)
			throws ServletException {
		String applicationName = req.getParameter("application");
		String callString = req.getParameter("call");

		try {
			String json = call(applicationName, callString);
			byte[] data = json.getBytes(Charset.forName("UTF-8"));
			resp.setContentType("application/json");
			resp.setContentLength(data.length);
			resp.getOutputStream().write(data);
		} catch (Exception e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	protected String call(String applicationName, String callString)
			throws Exception {
		return mwanzia.call(applicationName, callString);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		Map<String, String> configMap = new HashMap<String, String>();
		Enumeration<String> keys = config.getInitParameterNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			configMap.put(key, config.getInitParameter(key));
		}
		try {
			mwanzia = new Mwanzia(configMap);
		} catch (Exception e) {
			throw new ServletException("Unable to initialize Mwanzia: "
					+ e.getMessage(), e);
		}
	}
}
