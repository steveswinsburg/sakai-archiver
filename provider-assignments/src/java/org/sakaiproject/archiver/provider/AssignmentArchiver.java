package org.sakaiproject.archiver.provider;

import java.util.Collection;
import java.util.List;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.util.Jsonifier;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.Assignment.AssignmentAccess;
import org.sakaiproject.assignment.api.AssignmentContent;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;
import org.sakaiproject.service.gradebook.shared.GradebookService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the assignment tool
 */
@Slf4j
public class AssignmentArchiver implements Archiveable {

	private static final String ASSIGNMENT_TOOL = "sakai.assignment.grades";

	public void init() {
		ArchiverRegistry.getInstance().register(ASSIGNMENT_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(ASSIGNMENT_TOOL);
	}

	@Setter
	private GradebookService gradebookService;

	@Setter
	private GradebookExternalAssessmentService gradebookExternalService;

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private AssignmentService assignmentService;

	@Setter
	private ArchiverService archiverService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		List<Assignment> assignments = this.assignmentService.getListAssignmentsForContext(siteId);

		for (Assignment assignment : assignments) {
			SimpleAssignment simpleAssignment = new SimpleAssignment(assignment);
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleAssignment).getBytes(), assignment.getTitle() + ".json");
		}
		log.debug(assignments.toString());
	}

	/**
	 * Simplified helper class to represent an individual assignment item in a site
	 */
	public class SimpleAssignment {

		@Setter
		private String id;

		@Setter
		private AssignmentContent content;

		@Setter
		private String timeOpen;

		@Setter
		private String timeDue;

		@Setter
		private String lateAfter;

		@Setter
		private String acceptUntil;

		@Setter
		private String section;

		@Setter
		private boolean draft;

		@Setter
		private String creator;

		@Setter
		private String timeCreated;

		@Setter
		private List authors;

		@Setter
		private String instructions;

		@Setter
		private String lastModified;

		@Setter
		private String authorLastModified;

		@Setter
		private String title;

		@Setter
		private String status;

		@Setter
		private int position_order;

		@Setter
		private Collection groups;

		@Setter
		private AssignmentAccess access;

		@Setter
		private String gradeScale;

		@Setter
		private String gradeScaleMaxPoints;

		@Setter
		private String submissionType;

		@Setter
		private boolean allowResubmission;

		@Setter
		private String modelAnswerText;

		@Setter
		private String privateNoteText;

		@Setter
		private String allPurposeItemText;


		public SimpleAssignment(Assignment a) {
			super();
			if (a == null){
				return;
			}
			this.id = a.getId();
			this.timeOpen = a.getOpenTimeString();
			this.timeDue = a.getDueTimeString();
			this.lateAfter = a.getDropDeadTimeString();
			this.acceptUntil = a.getCloseTimeString();
			this.section = a.getSection();
			this.draft = a.getDraft();
			this.creator = a.getCreator();
			this.authors = a.getAuthors();
			this.authorLastModified = a.getAuthorLastModified();
			this.title = a.getTitle();
			this.status = a.getStatus();
			this.position_order = a.getPosition_order();
			this.groups = a.getGroups();
			this.access = a.getAccess();

		}
	}

}
