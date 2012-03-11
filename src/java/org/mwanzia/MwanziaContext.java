package org.mwanzia;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MwanziaContext {
	private HttpServletRequest request;
	private HttpServletResponse response;
	private ServletContext servletContext;

	public MwanziaContext(HttpServletRequest request,
			HttpServletResponse response, ServletContext servletContext) {
		super();
		this.request = request;
		this.response = response;
		this.servletContext = servletContext;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

}
