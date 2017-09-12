package org.sakaiproject.archiver.provider;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.provider.model.CsvData;
import org.sakaiproject.archiver.provider.model.StudentGradeInfo;
import org.sakaiproject.archiver.provider.util.FormatHelper;
import org.sakaiproject.archiver.provider.util.I18n;
import org.sakaiproject.archiver.provider.util.LastNameComparator;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import com.opencsv.CSVWriter;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the Gradebook tools
 *
 * Caller must have gradeall permission
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class GradebookArchiver implements Archiveable {

	private static final String GRADEBOOK_CLASSIC_TOOL = "sakai.gradebook.tool";
	private static final String GRADEBOOKNG_TOOL = "sakai.gradebookng";
	private static final String TOOL_NAME = "Gradebook";

	public void init() {
		ArchiverRegistry.getInstance().register(GRADEBOOK_CLASSIC_TOOL, this);
		ArchiverRegistry.getInstance().register(GRADEBOOKNG_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(GRADEBOOK_CLASSIC_TOOL);
		ArchiverRegistry.getInstance().unregister(GRADEBOOKNG_TOOL);
	}

	@Setter
	private GradebookService gradebookService;

	@Setter
	private ArchiverService archiverService;

	@Setter
	private SiteService siteService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final Gradebook gradebook = getGradebook(siteId);

		if (gradebook == null) {
			log.error("No gradebook in site: {} ", siteId);
			return;
		}

		// get all of the data
		final List<Assignment> assignments = getAssignments(gradebook);
		final List<String> studentUuids = getStudentUuids(siteId);
		final List<User> students = getUsers(studentUuids);
		final Map<String, CourseGrade> courseGrades = getCourseGrades(gradebook, studentUuids);

		final CsvData csvData = new CsvData();

		// set header
		final List<String> header = buildHeader(assignments);
		csvData.setHeader(header.toArray(new String[header.size()]));

		// this follows gradebookng's grade matrix pattern:
		// 1. course grade map contains record for every student so we can use that as the seed entry in the matrix
		// 2. then add assignment data to the matrix. Need to get the grades per assignment. Note that the assignment grade list might not
		// contain a record for each student.
		if (includeStudentContent) {
			final Map<String, StudentGradeInfo> matrix = new LinkedHashMap<>();
			addCourseGradesToMatrix(matrix, students, courseGrades);
			addAssignmentGradesToMatrix(matrix, studentUuids, assignments, gradebook);

			final List<StudentGradeInfo> grades = new ArrayList<>(matrix.values());

			grades.forEach(gradeInfo -> {
				final List<String> row = buildRow(gradeInfo, assignments);

				// add row to csv
				csvData.addRow(row.toArray(new String[row.size()]));
			});
		}

		// finalise the csv
		final byte[] csv = generateCsv(csvData);
		if (ArrayUtils.isNotEmpty(csv)) {
			this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, csv, "gradebook-export.csv");
		}

	}

	@Override
	public String getToolName(final String siteId) {
		// multiple tools here, use the hardcoded version. Could be swapped for just one of the toolIds...
		return TOOL_NAME;
	}

	/**
	 * Add course grades to the matrix
	 *
	 * @param matrix
	 * @param students
	 * @param courseGrades
	 */
	private void addCourseGradesToMatrix(final Map<String, StudentGradeInfo> matrix, final List<User> students,
			final Map<String, CourseGrade> courseGrades) {
		for (final User student : students) {
			final StudentGradeInfo gradeInfo = new StudentGradeInfo(student);
			final CourseGrade courseGrade = courseGrades.get(student.getId());
			gradeInfo.setCourseGrade(courseGrade);
			matrix.put(student.getId(), gradeInfo);
		}
	}

	/**
	 * Add assignment grades to the matrix
	 *
	 * @param matrix
	 * @param studentUuids
	 * @param assignments
	 * @param gradebook
	 */
	private void addAssignmentGradesToMatrix(final Map<String, StudentGradeInfo> matrix, final List<String> studentUuids,
			final List<Assignment> assignments, final Gradebook gradebook) {

		for (final Assignment assignment : assignments) {
			final List<GradeDefinition> defs = getGrades(gradebook, assignment, studentUuids);
			for (final GradeDefinition def : defs) {
				final StudentGradeInfo gradeInfo = matrix.get(def.getStudentUid());
				if (gradeInfo != null) {
					gradeInfo.addGrade(assignment.getId(), def.getGrade());
				}
			}
		}
	}

	/**
	 * Builder the header for the CSV
	 *
	 * @param assignments
	 * @return
	 */
	private List<String> buildHeader(final List<Assignment> assignments) {
		final List<String> header = new ArrayList<>();
		header.add(I18n.getString("export.header.studentId"));
		header.add(I18n.getString("export.header.studentName"));
		header.add(I18n.getString("export.headers.totalPoints"));
		header.add(I18n.getString("export.headers.courseGrade"));
		header.add(I18n.getString("export.headers.calculatedGrade"));
		header.add(I18n.getString("export.headers.gradeOverride"));
		assignments.forEach(a -> header.add(a.getName() + " [" + a.getPoints() + "]"));
		return header;
	}

	/**
	 * Build a row for each student
	 *
	 * @param gradeInfo the StudentGradeInfo object containing the data for the student
	 * @param assignments the list of assignments
	 * @return
	 */
	private List<String> buildRow(final StudentGradeInfo gradeInfo, final List<Assignment> assignments) {
		final List<String> row = new ArrayList<>();
		row.add(gradeInfo.getUserEid());
		row.add(gradeInfo.getDisplayName());

		row.add(FormatHelper.formatDoubleToDecimal(gradeInfo.getCourseGrade().getPointsEarned(), 2));
		row.add(gradeInfo.getCourseGrade().getMappedGrade());
		row.add(gradeInfo.getCourseGrade().getCalculatedGrade());
		row.add(gradeInfo.getCourseGrade().getEnteredGrade());

		// add assignments
		assignments.forEach(assignment -> {
			final String grade = gradeInfo.getGrades().get(assignment.getId());
			if (grade != null) {
				row.add(FormatHelper.trimZero(grade));
			} else {
				row.add(null);
			}
		});
		return row;
	}

	/**
	 * Get a list of gradeable users (ie students) as uuids
	 *
	 * @param siteId
	 * @return
	 */
	private List<String> getStudentUuids(final String siteId) {
		try {
			final Set<String> userIds = this.siteService.getSite(siteId).getUsersIsAllowed("gradebook.viewOwnGrades");
			return new ArrayList<>(userIds);
		} catch (final IdUnusedException e) {
			log.error("No users in site: {}", siteId);
			return Collections.emptyList();
		}
	}

	/**
	 * Hydrate the list of uuids into {@link User}s
	 *
	 * @param userUuids the list of uuids to lookup
	 * @return
	 */
	private List<User> getUsers(final List<String> userUuids) {
		final List<User> users = this.userDirectoryService.getUsers(userUuids);
		Collections.sort(users, new LastNameComparator());
		return users;
	}

	/**
	 * Get the gradebook
	 *
	 * @param siteId the siteId
	 * @return the gradebook for the site
	 */
	private Gradebook getGradebook(final String siteId) {
		try {
			return (Gradebook) this.gradebookService.getGradebook(siteId);
		} catch (final GradebookNotFoundException e) {
			return null;
		}
	}

	/**
	 * Get the assignments
	 *
	 * @param gradebook the gradebook
	 * @return
	 */
	private List<Assignment> getAssignments(final Gradebook gradebook) {
		return this.gradebookService.getViewableAssignmentsForCurrentUser(gradebook.getUid());
	}

	/**
	 * Get course grades. key = studentUuid, value = course grade
	 *
	 * @param gradebook
	 * @param studentUuids
	 * @return
	 */
	private Map<String, CourseGrade> getCourseGrades(final Gradebook gradebook, final List<String> studentUuids) {
		return this.gradebookService.getCourseGradeForStudents(gradebook.getUid(), studentUuids);
	}

	/**
	 * Get the list of grades
	 *
	 * @param gradebook
	 * @param assignment
	 * @param studentUuids
	 * @return
	 */
	private List<GradeDefinition> getGrades(final Gradebook gradebook, final Assignment assignment, final List<String> studentUuids) {
		return this.gradebookService.getGradesForStudentsForItem(gradebook.getUid(), assignment.getId(), studentUuids);
	}

	/**
	 * Generate the csv data as a byte[]
	 *
	 * @param csvData the collected CSV data
	 * @return
	 */
	private byte[] generateCsv(final CsvData csvData) {

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			final CSVWriter csvWriter = new CSVWriter(bufferedWriter);
			csvWriter.writeNext(csvData.getHeader());
			csvWriter.writeAll(csvData.getRows());
			csvWriter.close(); // auto closes the bufferedWriter
		} catch (final IOException e) {
			log.error("Error generating CSV data. Gradebook export cannot be written", e);
		}

		return outputStream.toByteArray();
	}

}
