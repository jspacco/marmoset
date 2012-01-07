package edu.umd.cs.submitServer.dao;

import java.util.List;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Student;

/** Dao that handles course registration actions. An instance applies to a specific Student.
 * 
 * @author rwsims
 *
 */
public interface RegistrationDao {
	public Student getStudent();
	
	/**
	 * Request that the dao's Student be registered for a course. Returns true if a request was
	 * successfully added, or if there is already a pending request. If the request has been denied or
	 * accepted, returns false.
	 * 
	 */
	public boolean requestRegistration(int coursePK);
	
	/**
	 * Return a list of Students waiting for registration in the given course. The dao's user must
	 * be an instructor for the course.
	 */
	public List<Student> getPendingRegistrations(int coursePK); 
	
	/** Returns a list of all courses the dao's Student has pending registrations for. */
	public List<Course> getPendingRequests();
	
	/**
	 * Accept a registration request. The dao's Student must be an instructor for the course, and the
	 * request must be pending.
	 * 
	 * @return false if the request was not pending, or if there was no such request. true otherwise.
	 */
	public boolean acceptRegistration(int coursePK, @Student.PK int studentPK);

	/**
	 * Accept a registration request. The dao's Student must be an instructor for the course, and the
	 * request must be pending.
	 * 
	 * @return false if the request was not pending, or if there was no such request. true otherwise.
	 */
	public boolean denyRegistration(int coursePK, @Student.PK int studentPK);
}
