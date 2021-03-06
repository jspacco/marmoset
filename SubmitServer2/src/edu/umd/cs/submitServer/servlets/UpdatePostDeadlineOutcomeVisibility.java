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

package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.submitServer.InvalidRequiredParameterException;
import edu.umd.cs.submitServer.RequestParser;

/**
 * UpdatePostDeadlineOutcomeVisibility
 *
 * @author jspacco
 */
public class UpdatePostDeadlineOutcomeVisibility extends SubmitServerServlet {

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to
	 * post.
	 *
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		RequestParser parser = new RequestParser(request,
				getSubmitServerServletLog(), strictParameterChecking());
		Connection conn = null;
		try {
			conn = getConnection();

			String newPostDeadlineOutcomeVisibility = parser
					.getCheckedParameter("newPostDeadlineOutcomeVisibility");
			getSubmitServerServletLog().debug(
					"newPostDeadlineOUtcomeVisibility = "
							+ newPostDeadlineOutcomeVisibility);
			if (!newPostDeadlineOutcomeVisibility
					.equals(Project.POST_DEADLINE_OUTCOME_VISIBILITY_EVERYTHING)
					&& !newPostDeadlineOutcomeVisibility
							.equals(Project.POST_DEADLINE_OUTCOME_VISIBILITY_NOTHING)) {
				throw new ServletException(
						"Only valid settings for the post-deadline "
								+ "outcome visibility of a project are '"
								+ Project.POST_DEADLINE_OUTCOME_VISIBILITY_EVERYTHING
								+ "' and '"
								+ Project.POST_DEADLINE_OUTCOME_VISIBILITY_NOTHING
								+ "'");
			}
			// TODO check the project deadline

			Project project = (Project) request.getAttribute("project");
			String currentVisibility = project
					.getPostDeadlineOutcomeVisibility();
			if (!newPostDeadlineOutcomeVisibility.equals(currentVisibility)) {
				project.setPostDeadlineOutcomeVisibility(newPostDeadlineOutcomeVisibility);
				project.update(conn);
			}
			String target = request.getContextPath()
					+ "/view/instructor/projectUtilities.jsp?projectPK="
					+ project.getProjectPK();
			response.sendRedirect(target);
		} catch (SQLException e) {
			throw new ServletException(e);
		} catch (InvalidRequiredParameterException e) {
			throw new ServletException(e);
		} finally {
			releaseConnection(conn);
		}

	}

}
