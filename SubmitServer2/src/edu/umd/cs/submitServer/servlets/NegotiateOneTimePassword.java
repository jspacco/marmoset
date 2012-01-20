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
 * Created on Jan 14, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.StudentSubmitStatus;
import edu.umd.cs.submitServer.ClientRequestException;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.SubmitServerUtilities;
import edu.umd.cs.submitServer.filters.AccessLogFilter;
import edu.umd.cs.submitServer.filters.ServletExceptionFilter;

/**
 * @author jspacco
 * 
 */
public class NegotiateOneTimePassword extends SubmitServerServlet {
    private static Logger accessLog;

    private Logger getAccessLog() {
        if (accessLog == null) {
            accessLog = Logger.getLogger(AccessLogFilter.class);
        }
        return accessLog;
    }


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean transactionSuccess = false;
        Connection conn = null;
        String courseKey = null;
        String courseName = null;
        String projectNumber = null;
        String loginName = null;
        String password = null;
        Project project = null;
        Student student = null;
        StudentRegistration studentRegistration = null;
      
        
        try {
            conn = getConnection();
            RequestParser parser = new RequestParser(request, getSubmitServerServletLog(), strictParameterChecking());
            courseKey = parser.getCheckedParameter("courseKey");
            projectNumber = parser.getCheckedParameter("projectNumber");
            
            loginName = parser.getCheckedParameter("loginName");
            if (loginName == null)
                loginName = parser.getCheckedParameter("campusUID");
           
            password = parser.getPasswordParameter("password");
            if (password == null)
                password = parser.getPasswordParameter("uidPassword");

           
            if (courseKey != null) {
                getAccessLog().info(
                        "NegotiateOneTimePassword attempt:\t" + loginName + "\t" + courseKey + "\t" 
                                + projectNumber);


                Course course = Course.lookupByCourseKey(courseKey, conn);
                if (course == null) {
                    String msg = "Could not find record for courseKey " + courseKey 
                            + "; you likely have an out of date .submit file";
                    ServletExceptionFilter.logErrorAndSendServerError(conn, request, response, msg, "login name: " + loginName, null);
                    return;
                }
                courseName = course.getCourseName();
                project = Project.lookupByCourseAndProjectNumber(course.getCoursePK(), projectNumber, conn);
                
                if (project == null) {
                    String msg = "Could not find record for project number " + projectNumber  + " in " + course.getCourseName()
                            + "; you likely have an out of date .submit file";
                    ServletExceptionFilter.logErrorAndSendServerError(conn, request, response, msg, "login name: " + loginName, null);
                    return;
                }
                
            } else {
                courseName = parser.getCheckedParameter("courseName");
                String semester = parser.getCheckedParameter("semester");
                String section = parser.getCheckedParameter("section");
                
                project = Project.lookupByCourseProjectSemester(courseName, section, projectNumber, semester, conn);
                if (project == null) {
                    
                    String msg = "Could not find record for project " + projectNumber + " in " + courseName + ", " + semester
                            + "; you likely have an out of date .submit file";
                    ServletExceptionFilter.logErrorAndSendServerError(conn, request, response, msg, "login name: " + loginName, null);
                    return;
                }
            }
            
           
            // authenticate the student and find their Student record
            student = PerformLogin.authenticateStudent(conn, loginName, password, skipAuthentication, getIAuthenticationService());

            // I need to do my own logging here-- AccessLogFilter cannot be
            // applied
            // to this servlet!
            getAccessLog().info(
                    "studentPK " + student.getStudentPK() + " requesting " + SubmitServerUtilities.extractURL(request));

            studentRegistration = StudentRegistration.lookupByStudentPKAndCoursePK(student.getStudentPK(), project.getCoursePK(),
                    conn);

            if (studentRegistration == null) {

                String msg = student.getFirstname() + " " + student.getLastname() + " is not registered for this " + courseName + ", "

                        + " If you changed your DirectoryID, please notify your instructor so that we can update the system.";
                ServletExceptionFilter.logErrorAndSendServerError(conn, request, response, msg, "login name: " + loginName, null);
                return;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        } catch (NamingException e) {
            throw new ServletException(e);
        } catch (ClientRequestException e) {
            String msg = String.format("failed to negotiate oneTime password for %s in %s %s: %s", loginName, courseName,
                    e.getMessage());
           
            ServletExceptionFilter.logErrorAndSendServerError(conn, request, response, msg, "login name: " + loginName, e);
           
            return;

        } finally {
            releaseConnection(conn);
        }
        conn = null;
        try {

            conn = getConnection();

            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            StudentSubmitStatus submitStatus = StudentSubmitStatus.createOrInsert(project.getProjectPK(),
                    studentRegistration.getStudentRegistrationPK(), conn);

           
            conn.commit();
            transactionSuccess = true;

            generateSubmitUser(response, student, studentRegistration, courseName, project, submitStatus);

            getAccessLog().info(
                    "studentPK " + studentRegistration.getStudentPK() + " successful "
                            + SubmitServerUtilities.extractURL(request));
            getAuthenticationLog().info(
                    "NegotiateOneTimePassword success:\t" + loginName + "\t" + courseName + "\t"
                            + projectNumber);

        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            rollbackIfUnsuccessfulAndAlwaysReleaseConnection(transactionSuccess, request, conn);
        }
    }


    public static void generateSubmitUser(HttpServletResponse response, Student student,
            StudentRegistration studentRegistration, String courseName, Project project, StudentSubmitStatus submitStatus) throws IOException {
        
        if (student.getStudentPK() != studentRegistration.getStudentPK())
            throw new IllegalArgumentException("Student and StudentRegistration do not match");
        String classAccount = studentRegistration.getClassAccount();
        String oneTimePassword = submitStatus.getOneTimePassword();

        // write out the classAccount/oneTimePassword pair
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "inline; filename=\".submitUser\"");
        PrintWriter out = response.getWriter();
        printProperty(out,"loginName" , student.getLoginName());
        
        printProperty(out,"classAccount" , classAccount);
        printProperty(out, "cvsAccount" , classAccount);
        printProperty(out,"oneTimePassword" , oneTimePassword);
        out.println();
        printComment(out, " for " + studentRegistration.getFullname());
        printComment(out, " course " + courseName);
        printComment(out, " project " + project.getProjectNumber() + " : " + project.getTitle());
        out.flush();
        out.close();
    }

    boolean skipAuthentication;
}
