package edu.umd.cs.submitServer.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import edu.umd.cs.submitServer.WebConfigProperties;

public class SkipAuthenticationParameterFilter implements Filter {
	private static final WebConfigProperties webProperties = WebConfigProperties.get();
	private boolean skipAuthentication;
	
	@Override
  public void init(FilterConfig filterConfig) throws ServletException {
		skipAuthentication = "true".equals(webProperties.getProperty("authentication.skip"));
  }

	@Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
	  request.setAttribute("skipAuthentication", skipAuthentication);
	  chain.doFilter(request, response);
  }

	@Override
  public void destroy() {}
}
