package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchResponse;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.submitServer.SubmitServerConstants;
import edu.umd.cs.submitServer.WebConfigProperties;

/**
 * Servlet for verifying OpenID responses from the OP.
 * 
 * @author Ryan W Sims (rwsims@umd.edu)
 * @see InitiateOpenId
 * 
 */
public class VerifyOpenId extends SubmitServerServlet {
	private static final WebConfigProperties webProperties = WebConfigProperties.get();
	private static final String GOOGLE_ID_PREFIX = "https://www.google.com/accounts/o8/id";
	private static final int MAX_ID_LENGTH = 80;
	
	public static String url(String contextPath) {
		return contextPath + "/authenticate/openid/verify";
	}

	private final transient ConsumerManager consumerManager = new ConsumerManager();

	@Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
		boolean skipAuthentication = "true".equals(webProperties.getProperty("authentication.skip"));
		String uid = null;
		String loginName = null;
		Map<String, String> openidAx = Maps.newTreeMap();
		if (skipAuthentication) {
			uid = req.getParameter("uid");
			loginName = req.getParameter("login_name");
		}
	    if (uid == null && loginName == null) {
	        uid = verifyIdentity(req, openidAx);
	    }
		Connection conn = null;
		try {
	    conn = getConnection();
	    Student student;
	    if (uid != null) { 
	        student = Student.lookupByCampusUID(uid, conn);
	    } else {
	    	uid = loginName;
	        student = Student.lookupByLoginName(loginName, conn);
	    }
	    
	    String targetUrl = req.getParameter("marmoset.target");
	    if (Strings.isNullOrEmpty(targetUrl)) {
	    	targetUrl = Util.urlEncode(req.getContextPath() + "/view/index.jsp");
	    }
	    if (student == null) {
	    	req.getSession().setAttribute(SubmitServerConstants.OPENID_AX_MAP, openidAx);
	    	// Redirect to account registration page.
	  		resp.sendRedirect(String.format("%s/%s?uid=%s&target=%s",
	  		                                req.getContextPath(),
	  		                                "authenticate/openid/register.jsp",
	  		                                uid,
	  		                                targetUrl));
	  		return;
	    }
	    PerformLogin.setUserSession(req.getSession(), student, conn);
	    resp.sendRedirect(Util.urlDecode(targetUrl));
    } catch (SQLException e) {
	    throw new ServletException(e);
    } finally {
    	releaseConnection(conn);
    }
  }

	private String verifyIdentity(HttpServletRequest req, Map<String, String> openidAx) throws ServletException {
		ParameterList openIdResp = new ParameterList(req.getParameterMap());
		DiscoveryInformation discovered = (DiscoveryInformation) req.getSession()
		                                                            .getAttribute(SubmitServerConstants.OPENID_DISCOVERED);

		StringBuffer receivingUrl = req.getRequestURL();
		String queryString = req.getQueryString();
		if (!Strings.isNullOrEmpty(queryString)) {
			receivingUrl.append("?").append(queryString);
		}
		VerificationResult verification;
		String uid = null;
		try {
			verification = consumerManager.verify(receivingUrl.toString(), openIdResp, discovered);

			Identifier verified = verification.getVerifiedId();
			if (verified == null)
			    throw new ServletException("OpenID authentication declined or failed");
			
			getSubmitServerServletLog().info("Verified OpenID " + verified.getIdentifier());
			AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();
			
			String email = null;
			if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
				FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
				Logger logger = getSubmitServerServletLog();
				
				email = (String) fetchResp.getAttributeValue("email");
				if (!Strings.isNullOrEmpty(email)) {
					logger.info("Got email: " + email);
					openidAx.put("email", email);
				}
				
				String firstname = (String) fetchResp.getAttributeValue("firstname");
				if (!Strings.isNullOrEmpty(firstname)) {
					logger.info("Got first name: " + firstname);
					openidAx.put("firstname", firstname);
				}
				
				String lastname = (String) fetchResp.getAttributeValue("lastname");
				if (!Strings.isNullOrEmpty(lastname)) {
					logger.info("Got last name: " + lastname);
					openidAx.put("lastname", lastname);
				}
			}
			
			String identifier = verified.getIdentifier();
			if (identifier.startsWith(GOOGLE_ID_PREFIX)) {
				getSubmitServerServletLog().info("Found Google identifier, using email instead");
				// Google's openid identifiers are domain-specific, so sometimes they change subtly. In the case of
				// a google account, we use the email address (we can always rely on getting one) instead of the id.
				Preconditions.checkState(!Strings.isNullOrEmpty(email), "Could not get email from google!");
				uid = email;
			} else {
				uid = (identifier.length() > MAX_ID_LENGTH) ? hashOpenId(verified.getIdentifier()) : identifier;
			}
			getSubmitServerServletLog().info("Using uid " + uid);
		} catch (MessageException e) {
			throw new ServletException(e);
		} catch (DiscoveryException e) {
			throw new ServletException(e);
		} catch (AssociationException e) {
			throw new ServletException(e);
		}
		return uid;
	}

	/**
	 * SHA-1 hash an OpenID identifier, since OpenID puts no restriction on the length of an
	 * identifier.
	 */
	private String hashOpenId(String identifier) throws ServletException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(identifier.getBytes());
			String hashed = String.format("%040x", new BigInteger(1, digest.digest()));
			getSubmitServerServletLog().info("Hashed openid " + identifier + " to " + hashed);
			return hashed;
		} catch (NoSuchAlgorithmException e) {
			throw new ServletException("Couldn't hash OpenID identifier");
		}
	}
}
