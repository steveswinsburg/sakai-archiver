package org.sakaiproject.archiver.provider;

import java.util.Collection;
import java.util.List;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.model.AssignmentAllPurposeItem;
import org.sakaiproject.assignment.api.model.AssignmentModelAnswerItem;
import org.sakaiproject.assignment.api.model.AssignmentNoteItem;
import org.sakaiproject.assignment.api.model.AssignmentSupplementItemService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the assignment tool
 */
@Slf4j
public class AssignmentsArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.assignment.grades";
	private static final String TOOL_NAME = "Assignments";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private GradebookService gradebookService;

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private AssignmentService assignmentService;

	@Setter
	private ArchiverService archiverService;

	@Setter
	private SiteService siteService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private AssignmentSupplementItemService assignmentSupplementItemService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final List<Assignment> assignments = this.assignmentService.getListAssignmentsForContext(siteId);

		for (final Assignment assignment : assignments) {

			// archive the assignment data
			final SimpleAssignment simpleAssignment = new SimpleAssignment(assignment);
			final String html = Htmlifier.addSiteHeader(Htmlifier.toHtml(simpleAssignment),
					this.archiverService.getSiteHeader(siteId, TOOL_ID));
			this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, html.getBytes(), "details.html", assignment.getTitle());

			// archive the attachments for the assignment
			final String attachmentDir = assignment.getTitle() + "/attachments";
			archiveAttachments(assignment.getContent().getAttachments(), attachmentDir, archiveId, siteId);

			// if we want student content, archive the submissions for the assignment
			if (includeStudentContent) {
				archiveSubmissions(assignment, archiveId, siteId);
			}
		}

		// archive the grades spreadsheet for the site
		archiveGradesSpreadsheet(archiveId, siteId);
	}

	/**
	 * Get the submissions for an assignment, and any feedback from the instructor, and archive them
	 *
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 */
	@SuppressWarnings("unchecked")
	private void archiveSubmissions(final Assignment assignment, final String archiveId, final String siteId) {
		final List<AssignmentSubmission> submissions = this.assignmentService.getSubmissions(assignment);

		for (final AssignmentSubmission submission : submissions) {

			final String submissionSubdirs = getSubDirs(assignment, submission.getSubmitterId(), "submission");

			// archive the attachments for this submission
			archiveAttachments(submission.getSubmittedAttachments(), submissionSubdirs, archiveId, siteId);

			// get other data associated with this submission
			if (submission.getTimeSubmitted() != null) {
				final SimpleSubmission submissionData = new SimpleSubmission(submission, assignment.isGroup());
				final String html = Htmlifier.addSiteHeader(Htmlifier.toHtml(submissionData),
						this.archiverService.getSiteHeader(siteId, TOOL_ID));
				this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, html.getBytes(), "submission.html", submissionSubdirs);
			}

			// get the feedback, if this submission has been graded
			if (submission.getGraded()) {
				archiveFeedback(submission, assignment, archiveId, siteId);
			}
		}
	}

	/**
	 * Helper method to archive feedback for a submission
	 *
	 * @param submission
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 */
	@SuppressWarnings("unchecked")
	private void archiveFeedback(final AssignmentSubmission submission, final Assignment assignment, final String archiveId,
			final String siteId) {

		// archive the attachments provided by the instructor when grading the submission
		final String feedbackSubdirs = getSubDirs(assignment, submission.getSubmitterId(), "feedback");
		archiveAttachments(submission.getFeedbackAttachments(), feedbackSubdirs, archiveId, siteId);

		// archive other data associated with this feedback
		final SimpleFeedback feedback = new SimpleFeedback(submission);
		final String html = Htmlifier.addSiteHeader(Htmlifier.toHtml(feedback), this.archiverService.getSiteHeader(siteId, TOOL_ID));
		this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, html.getBytes(), "feedback.html", feedbackSubdirs);
	}

	/**
	 * Archive a list of attachments
	 *
	 * @param attachments
	 * @param subdirs
	 * @param archiveId
	 * @param siteId
	 */
	private void archiveAttachments(final List<Reference> attachments, final String subdirs, final String archiveId, final String siteId) {
		for (final Reference attachment : attachments) {
			try {
				archiveAttachment(attachment, archiveId, siteId, subdirs);
			} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
				log.error("Error getting attachment: " + attachment.getId());
			}
		}
	}

	/**
	 * Helper method to archive an attachment
	 *
	 * @param attachment
	 * @param archiveId
	 * @param siteId
	 * @param subdir
	 * @throws ServerOverloadException
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws TypeException
	 */
	private void archiveAttachment(final Reference attachment, final String archiveId, final String siteId, final String subdir)
			throws ServerOverloadException, PermissionException, IdUnusedException, TypeException {
		final byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
		this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, attachmentBytes,
				attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName()), subdir);
	}

	/**
	 * Helper method to archive the grades spreadsheet for the site
	 *
	 * @param archiveId
	 * @param siteId
	 */
	private void archiveGradesSpreadsheet(final String archiveId, final String siteId) {

		// Note: The AssignmentService contains a method 'gradesSpreadsheetReference' but this cannot be used in this context.
		// GradeSheetExporter#getGradesSpreadsheet will not accept that format.
		// Below is what is needed.
		final String spreadsheetReference = String.join(Entity.SEPARATOR, AssignmentService.REF_TYPE_GRADES, siteId);

		byte[] gradesSpreadsheet;
		try {

			gradesSpreadsheet = this.assignmentService.getGradesSpreadsheet(spreadsheetReference);
			if (gradesSpreadsheet != null) {
				this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, gradesSpreadsheet, "grades.xls");
			}
		} catch (IdUnusedException | PermissionException e) {
			log.error("Error getting grades spreadsheet for site {} ", siteId);
		}
	}

	/**
	 * Get the subdirectory structure for a submission or feedback item
	 *
	 * @param assignmentTitle
	 * @param submitterId
	 * @param folderName
	 * @return subdirectory string
	 */
	private String getSubDirs(final Assignment assignment, final String submitterId, final String folderName) {

		// Get the user associated with this submitterId
		final User user = getUser(submitterId);
		if (user != null) {
			return assignment.getTitle() + "/submissions/" + user.getSortName() + "/" + folderName;
		}

		// If a user wasn't found, maybe it's a group submission
		final Group group = getGroup(assignment.getContext(), submitterId);
		if (group != null) {
			return assignment.getTitle() + "/submissions/" + group.getTitle() + "/" + folderName;
		}

		// Neither a user or group could be found, use submitterId as folder name
		log.error("Neither a user or group name could not be found for submitterId: {}", submitterId);
		return assignment.getTitle() + "/" + submitterId + "/submission";
	}

	/**
	 * Helper method to get the User associated with a submitterId
	 *
	 * @param submitterId
	 * @return user
	 */
	private User getUser(final String submitterId) {
		try {
			return this.userDirectoryService.getUser(submitterId);
		} catch (final UserNotDefinedException e) {
			return null;
		}
	}

	/**
	 * Helper method to get the Group associated with a submitterId
	 *
	 * @param siteId
	 * @param submitterId
	 * @return group
	 */
	private Group getGroup(final String siteId, final String submitterId) {
		try {
			return this.siteService.getSite(siteId).getGroup(submitterId);
		} catch (final IdUnusedException e) {
			return null;
		}
	}
}
