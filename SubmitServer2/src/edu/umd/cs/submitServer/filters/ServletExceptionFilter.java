/**
 * Marmoset: a student project snapshot, submission, testing and code review
 * system developed by the Univ. of Maryland, College Park
 * 
 * Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 * by William Pugh. See http://marmoset.cs.umd.edu/
 * 
 * Copyright 2005 - 2011, Univ. of Maryland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/*
 * Created on Jan 24, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

import javax.annotation.CheckForNull;
import javax.naming.NamingException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.ServerError;
import edu.umd.cs.marmoset.modelClasses.ServerError.Kind;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.Submission;
import edu.umd.cs.submitServer.UserSession;

/**
 * Catches the ServletExceptions and prints them to the response as text.
 *
 * @author jspacco
 */
public class ServletExceptionFilter extends SubmitServerFilter {
	private static Logger servletExceptionLog;

	private Logger getServletExceptionLog() {
		if (servletExceptionLog == null) {
			servletExceptionLog = Logger
					.getLogger("edu.umd.cs.submitServer.logging.servletExceptionLog");
		}
		return servletExceptionLog;
	}
	public static String nullSafeToString(Object x) {
		if (x == null)
			return null;
		return x.toString();
	}

	public static String getOptionalParameterAsString(ServletRequest req,
			String name) {
		return nullSafeToString(req.getAttribute(name));
	}


	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		try {
			chain.doFilter(request, response);
		} catch (Exception e) {
			logErrorAndUpdateResponse(request, response, e);
		}
	}
    
	
	   public <E extends Throwable> void logExceptionAndRethrow(HttpServletRequest request, 
	            E e) throws E {
	       logErrorAndUpdateResponse(request, null, e);
	       throw e;
	   }
	 
	
    public  void logErrorAndUpdateResponse(HttpServletRequest request, 
           @CheckForNull  HttpServletResponse response, Throwable e) {
        getServletExceptionLog().error(e.getMessage(), e);

        // Get the most specific non-null message.
        Throwable cause = e.getCause();
        if (e instanceof ServletException && cause != null) {
        	  e = cause;
        	  cause = e.getCause();
        }
        String message = e.getMessage();
        if (cause != null) {
        	if (cause.getMessage() != null)
        		message = e.getMessage();
        }
        if (message == null || message.isEmpty())
        	message = e.getClass().getName();

        // TODO can check for other exception sub-types and http response
        // codes
        if (response != null && cause instanceof NamingException) {
        	message += "\nThe LDAP authentication system is not responding.  "
        			+ "Please try again in a couple of minutes";
        }

        Connection conn = null;
        try {
            conn = getConnection();
            logErrorAndSendServerError(conn, ServerError.Kind.EXCEPTION, request, response,message, null, e);
        } catch (Exception t) {
            getSubmitServerFilterLog().warn(t);
        } finally {
            releaseConnection(conn);
        }
    }
    
    public static void logError(Connection conn, 
            Kind kind, 
            HttpServletRequest request,
          String message, @CheckForNull String type, @CheckForNull Throwable e) {
        logErrorAndSendServerError(conn, kind, request, null, message, type, e);
    }

    public static void logErrorAndSendServerError(Connection conn, 
            Kind kind, 
            HttpServletRequest request,
            @CheckForNull HttpServletResponse response, String message,
            @CheckForNull String logOnlyMessage, @CheckForNull Throwable e) {

        try {
        String referer = request.getHeader("referer");
        String remoteHost =  SubmitServerFilter.getRemoteHost(request);
        HttpSession session = request.getSession();
        UserSession userSession = session == null ? null : ((UserSession) session
                .getAttribute(USER_SESSION));
    
        String type = null;
        if (logOnlyMessage!= null)
            type = logOnlyMessage;
        else if (e != null)
            type = e.getClass().getSimpleName();
        
        if (message == null) {
            if (type != null)
                message = type;
            else if (e != null)
                message = e.getClass().getSimpleName();
        }

        if (e == null)
            e = new RuntimeException("No exception provided");
        String requestURI = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        @Student.PK Integer userPK = userSession != null ? userSession.getStudentPK() : null;
        Student student = (Student) request.getAttribute(STUDENT);
        StudentRegistration studentRegistration = (StudentRegistration) request.getAttribute(STUDENT_REGISTRATION);
        @Student.PK Integer studentPK = userPK;
        if (studentPK == null) {
          if (student != null)
            studentPK = student.getStudentPK();
          else if (studentRegistration != null)
            studentPK = studentRegistration.getStudentPK();
        }
        
        if (userPK == null)
          userPK = studentPK;
        
        Project project = (Project) request.getAttribute(PROJECT);
        
        @Project.PK Integer projectPK = null;
        if (project != null)
          projectPK = project.getProjectPK();
          
        Submission submission = (Submission) request.getAttribute(SUBMISSION);
        
        @Submission.PK Integer submissionPK = null;
        if (submission != null)
          submissionPK = submission.getSubmissionPK();
        
        ServerError.insert(conn,kind, userPK, studentPK,  /* course */ null, projectPK, 
                submissionPK, /* code */ null, message, type, ServletExceptionFilter.class.getName(), requestURI,
                request.getQueryString(), remoteHost, referer, userAgent, e);

        if (response != null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println(message);
            out.close();
        }
        } catch (Exception e2) {
            getSubmitServerFilterLog().warn(e2);
        }
    }

	private static void printStacks(int numStacks, Throwable e, PrintWriter out) {
		if (e.getMessage() != null)
			out.println(e.getClass().getName());
		StackTraceElement[] trace = e.getStackTrace();
		for (int ii = 0; ii < numStacks && ii < trace.length; ii++)
			out.println("   " + trace[ii]);
	}
}
