package org.sakaiproject.archiver.model;

import java.util.HashMap;
import java.util.Map;

import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.user.api.User;

import lombok.Getter;
import lombok.Setter;

public class StudentGradeInfo {

	@Getter
	@Setter
	private String userId;

	@Getter
	@Setter
	private String userEid;

	@Getter
	@Setter
	private String displayName;

	/**
	 * Map of grades. Does not handle comments
	 */
	@Getter
	private final Map<Long, String> grades;

	@Getter
	@Setter
	private CourseGrade courseGrade;

	public StudentGradeInfo(final User u) {
		this.userId = u.getId();
		this.userEid = u.getEid();
		this.displayName = u.getDisplayName();
		this.grades = new HashMap<Long, String>();
	}

	/**
	 * Add a grade to the list
	 *
	 * @param assignmentId the id of the assignment
	 * @param grade the grade to add
	 */
	public void addGrade(final long assignmentId, final String grade) {
		this.grades.put(assignmentId, grade);

	}

}
