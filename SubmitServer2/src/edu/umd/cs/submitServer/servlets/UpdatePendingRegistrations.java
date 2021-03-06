package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.submitServer.SubmitServerConstants;
import edu.umd.cs.submitServer.dao.RegistrationDao;
import edu.umd.cs.submitServer.dao.impl.MySqlRegistrationDaoImpl;

/** Accepts or rejects pending registrations for course. 
 * 
 * @author rwsims
 *
 */
public class UpdatePendingRegistrations extends SubmitServerServlet {
	private static final Pattern requestParam = Pattern.compile("request-pk-(\\d+)");
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
		Student user = (Student) req.getAttribute(SubmitServerConstants.USER);
		RegistrationDao dao = new MySqlRegistrationDaoImpl(user, getDatabaseProps());
		
		int coursePK = Course.asPK(Integer.parseInt(Preconditions.checkNotNull(req.getParameter("course"))));
		for (Entry<String, String[]> entry : (Set<Entry<String, String[]>>) req.getParameterMap().entrySet()) {
			Matcher matcher = requestParam.matcher(entry.getKey());
			if (!matcher.matches()) {
				continue;
			}
			@Student.PK int studentPK = Student.asPK(Integer.parseInt(matcher.group(1)));
			Preconditions.checkState(entry.getValue().length == 1, "Should get exactly 1 value for each registration request.");
			String action = entry.getValue()[0];
			if (action.equals("accept")) {
				String section = req.getParameter("section-" + studentPK);
				if (!Strings.isNullOrEmpty(section)) {
					dao.acceptRegistration(coursePK, studentPK, section);
				} else {
					dao.acceptRegistration(coursePK, studentPK);
				}
			} else if (action.equals("reject")) {
				dao.denyRegistration(coursePK, studentPK);
			} else {
				throw new IllegalArgumentException("Unknown action for registration request.");
			}
		}
		resp.sendRedirect(req.getContextPath() + "/view/instructor/course.jsp?coursePK=" + coursePK);
	}
}
