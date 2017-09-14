package org.sakaiproject.archiver.provider;

import java.util.List;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
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

	private String attachmentsHtml;

	private String toolName;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final List<Assignment> assignments = this.assignmentService.getListAssignmentsForContext(siteId);

		this.toolName = getToolName(siteId);

		for (final Assignment assignment : assignments) {

			// clear the atttachmentsHtml string in case it contains attachments from a previous assignment
			this.attachmentsHtml = "";

			// archive the attachments for the assignment
			archiveAttachments(assignment.getContent().getAttachments(), assignment.getTitle(), archiveId, siteId, "/attachments");
			if (!assignment.getContent().getAttachments().isEmpty()) {
				finaliseAttachmentsHtml();
			}

			// archive the assignment data
			final String detailsHtml = getDetailsAsHtml(assignment);
			final String finalDetailsHtml = Htmlifier.toHtml(detailsHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
			this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalDetailsHtml.getBytes(), "details.html",
					assignment.getTitle());

			// if we want student content, archive the submissions for the assignment
			if (includeStudentContent) {
				archiveSubmissions(assignment, archiveId, siteId);
			}
		}

		// archive the grades spreadsheet for the site
		archiveGradesSpreadsheet(archiveId, siteId);
	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
	}

	/**
	 * Construct the details html string
	 *
	 * @param assignment
	 * @return
	 */
	private String getDetailsAsHtml(final Assignment assignment) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>" + assignment.getTitle() + "</h2>");

		if (assignment.getContent() != null) {
			sb.append("<p>" + assignment.getContent().getInstructions() + "</p>");
		}

		sb.append("<p>Due at: " + assignment.getDueTimeString() + "</p>");

		if (assignment.getContent() != null) {
			sb.append("<p>Maximum score: " + assignment.getContent().getMaxGradePointDisplay() + " "
					+ assignment.getContent().getTypeOfGradeString(assignment.getContent().getTypeOfGrade()) + "</p>");
			//not supported in 10.4:
			//sb.append("<p>Submission type: " + assignment.getContent().getTypeOfSubmissionString() + "</p>");
		}

		sb.append("<p>Attachment(s): " + this.attachmentsHtml + "</p>");

		return sb.toString();
	}

	/**
	 * Get the submissions for an assignment, and any feedback from the instructor, and archive it
	 *
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 */
	@SuppressWarnings("unchecked")

	private void archiveSubmissions(final Assignment assignment, final String archiveId, final String siteId) {
		final List<AssignmentSubmission> submissions = this.assignmentService.getSubmissions(assignment);

		for (final AssignmentSubmission submission : submissions) {

			// clear the atttachmentsHtml string
			this.attachmentsHtml = "";

			final String submissionSubdirs = getSubDirs(assignment, submission.getSubmitterId());

			// archive the attachments for this submission
			archiveAttachments(submission.getSubmittedAttachments(), submissionSubdirs, archiveId, siteId, "/submission");
			if (!submission.getSubmittedAttachments().isEmpty()) {
				finaliseAttachmentsHtml();
			}

			// get other data associated with this submission
			if (submission.getTimeSubmitted() != null) {
				final String submissionHtml = getSubmissionAsHtml(submission);
				final String finalSubmissionHtml = Htmlifier.toHtml(submissionHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
				this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalSubmissionHtml.getBytes(), "submission.html",
						submissionSubdirs);
			}

			// archive the feedback attachments, if there are any
			if (submission.getGraded() && !submission.getFeedbackAttachments().isEmpty()) {
				this.attachmentsHtml = "";
				archiveAttachments(submission.getFeedbackAttachments(), submissionSubdirs, archiveId, siteId, "/feedback");
				finaliseAttachmentsHtml();
			}
		}
	}

	/**
	 * Construct the submission html string
	 *
	 * @param submission
	 * @return submissionHtml
	 */
	private String getSubmissionAsHtml(final AssignmentSubmission submission) {

		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>" + submission.getAssignment().getTitle() + "</h2>");

		final User user = getUser(submission.getSubmitterId());
		if (user != null) {
			sb.append("<p>" + user.getEid() + "</p>");
		}

		sb.append("<p>" + submission.getTimeSubmittedString() + "</p>");
		sb.append("<p>" + submission.getSubmittedText() + "</p>");
		sb.append("<p>" + this.attachmentsHtml + "</p>");

		if (submission.getGraded()) {
			sb.append("<p>Instructor Feedback: " + submission.getFeedbackComment() + "</p>");
		}

		return sb.toString();
	}

	/**
	 * Archive a list of attachments
	 *
	 * @param attachments
	 * @param subdirs
	 * @param archiveId
	 * @param siteId
	 */
	private void archiveAttachments(final List<Reference> attachments, final String subdirs, final String archiveId, final String siteId,
			final String finalFolder) {
		for (final Reference attachment : attachments) {
			try {
				archiveAttachment(attachment, archiveId, siteId, subdirs, finalFolder);
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
	private void archiveAttachment(final Reference attachment, final String archiveId, final String siteId, final String subdir,
			final String finalFolder)
			throws ServerOverloadException, PermissionException, IdUnusedException, TypeException {
		final byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
		final String attachmentName = attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName());
		this.archiverService.archiveContent(archiveId, siteId, this.toolName, attachmentBytes, attachmentName, subdir + finalFolder);
		addToAttachmentsHtml(finalFolder, attachmentName);
	}

	/**
	 * Set the html string that contains a list of attachment hyperlinks
	 *
	 * @param attachmentLocation
	 * @param attachmentName
	 */
	private void addToAttachmentsHtml(final String attachmentLocation, final String attachmentName) {

		final String attachmentHyperlink = "<li><a href=\"./" + attachmentLocation + "/" + attachmentName + "\">" + attachmentName
				+ "</a></li>";
		this.attachmentsHtml += attachmentHyperlink;
	}

	/**
	 * Finalise the attachments html string by surrounding it by unordered list tags
	 */
	private void finaliseAttachmentsHtml() {

		this.attachmentsHtml = "<ul style=\"list-style: none;padding-left:0;\">" + this.attachmentsHtml + "</ul>";
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
				this.archiverService.archiveContent(archiveId, siteId, this.toolName, gradesSpreadsheet, "grades.xls");
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
	private String getSubDirs(final Assignment assignment, final String submitterId) {

		// Get the user associated with this submitterId
		final User user = getUser(submitterId);
		if (user != null) {
			return assignment.getTitle() + "/submissions/" + user.getSortName();
		}

		// If a user wasn't found, maybe it's a group submission
		final Group group = getGroup(assignment.getContext(), submitterId);
		if (group != null) {
			return assignment.getTitle() + "/submissions/" + group.getTitle();
		}

		// Neither a user or group could be found, use submitterId as folder name
		log.error("Neither a user or group name could not be found for submitterId: {}", submitterId);
		return assignment.getTitle() + "/" + submitterId;
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
