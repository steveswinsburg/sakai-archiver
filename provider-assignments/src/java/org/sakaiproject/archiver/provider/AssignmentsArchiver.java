package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.archiver.util.Sanitiser;
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

	private String toolName;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final List<Assignment> assignments = this.assignmentService.getListAssignmentsForContext(siteId);

		// List to hold the names of the assignments for this site
		final List<String> assignmentNames = new ArrayList<>();

		this.toolName = getToolName(siteId, TOOL_ID);

		for (final Assignment assignment : assignments) {

			assignmentNames.add(assignment.getTitle());

			String assignmentAttachmentsHtml = "";

			// archive the attachments for the assignment
			assignmentAttachmentsHtml = archiveAttachments(assignment.getContent().getAttachments(), new String[] { assignment.getTitle() },
					archiveId, siteId, "attachments", assignmentAttachmentsHtml);

			// archive the assignment data
			final String detailsHtml = getDetailsAsHtml(assignment, assignmentAttachmentsHtml);
			final String finalDetailsHtml = Htmlifier.toHtml(detailsHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
			this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalDetailsHtml.getBytes(), "details.html",
					assignment.getTitle());

			// if we want student content, archive the submissions for the assignment
			if (includeStudentContent) {
				archiveSubmissions(assignment, archiveId, siteId);
			}
		}

		// save an index file
		final String indexHtml = getIndexHtml(assignmentNames);
		final String finalIndexHtml = Htmlifier.toHtml(indexHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
		this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalIndexHtml.getBytes(), "Assignment_List.html");

		// archive the grades spreadsheet for the site
		archiveGradesSpreadsheet(archiveId, siteId);
	}

	@Override
	public String getToolName(final String siteId, String toolId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
	}

	/**
	 * Construct the details html string
	 *
	 * @param assignment
	 * @param attachmentsHtml
	 * @return detailsHtml
	 */
	private String getDetailsAsHtml(final Assignment assignment, final String attachmentsHtml) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>" + assignment.getTitle() + "</h2>");

		if (assignment.getContent() != null) {
			sb.append("<p>" + assignment.getContent().getInstructions() + "</p>");
		}

		sb.append("<p>Due at: " + assignment.getDueTimeString() + "</p>");

		if (assignment.getContent() != null) {
			final String gradingScale = assignment.getContent().getTypeOfGradeString();
			if (StringUtils.equals(gradingScale, "Points")) {
				sb.append("<p>Maximum score: " + assignment.getContent().getMaxGradePointDisplay() + " " + gradingScale + "</p>");
			} else {
				sb.append("<p>Grading scale: " + gradingScale + "</p>");
			}
			sb.append("<p>Submission type: " + assignment.getContent().getTypeOfSubmissionString() + "</p>");
		}

		if (StringUtils.isNotBlank(attachmentsHtml)) {
			sb.append("<p>Attachment(s): <ul style=\"list-style: none;padding-left:0;\">" + attachmentsHtml + "</ul></p>");
		}

		return sb.toString();
	}

	private String getIndexHtml(final List<String> assignmentNames) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>Assignment List</h2>");
		for (final String assignmentName : assignmentNames) {
			final String sanitisedName = Sanitiser.sanitise(assignmentName);
			sb.append("<p><a href=\"" + sanitisedName + "/details.html\">" + sanitisedName + "</a></p>");
		}

		sb.append("<p>Grades: <a href=\"./grades.xls\">grades.xls</a></p>");

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

			String submissionAttachmentsHtml = "";

			final String[] submissionSubdirs = getSubDirs(assignment, submission.getSubmitterId());

			// archive the attachments for this submission
			submissionAttachmentsHtml = archiveAttachments(submission.getSubmittedAttachments(), submissionSubdirs, archiveId, siteId,
					"submission", submissionAttachmentsHtml);

			// archive the feedback attachments, if there are any
			String feedbackAttachmentsHtml = "";
			if (submission.getGraded() && !submission.getFeedbackAttachments().isEmpty()) {
				feedbackAttachmentsHtml = archiveAttachments(submission.getFeedbackAttachments(), submissionSubdirs, archiveId, siteId,
						"feedback", feedbackAttachmentsHtml);
			}

			// archive this submission
			if (submission.getTimeSubmitted() != null) {
				final String submissionHtml = getSubmissionAsHtml(submission, submissionAttachmentsHtml, feedbackAttachmentsHtml);
				final String finalSubmissionHtml = Htmlifier.toHtml(submissionHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
				this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalSubmissionHtml.getBytes(), "submission.html",
						submissionSubdirs);
			}
		}
	}

	/**
	 * Construct the submission html string
	 *
	 * @param submission
	 * @param feedbackAttachmentsHtml
	 * @return submissionAttachmentsHtml
	 */
	private String getSubmissionAsHtml(final AssignmentSubmission submission, final String submissionAttachmentsHtml,
			final String feedbackAttachmentsHtml) {

		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>" + submission.getAssignment().getTitle() + "</h2>");

		final User user = getUser(submission.getSubmitterId());
		if (user != null) {
			sb.append("<p>" + user.getEid() + "</p>");
		}

		sb.append("<p>" + submission.getTimeSubmittedString() + "</p>");
		sb.append("<p>" + submission.getSubmittedText() + "</p>");
		sb.append("<p><ul style=\"list-style: none;padding-left:0;\">" + submissionAttachmentsHtml + "</ul></p>");

		if (submission.getGraded()) {
			sb.append("<p>Instructor Feedback: " + submission.getFeedbackComment() + "</p>");
			sb.append("<p><ul style=\"list-style: none;padding-left:0;\">" + feedbackAttachmentsHtml + "</ul></p>");
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
	 * @param finalFolder
	 * @param attachmentsHtml
	 * @return attachmentsHtml
	 */
	private String archiveAttachments(final List<Reference> attachments, final String[] subdirs, final String archiveId,
			final String siteId, final String finalFolder, final String attachmentsHtml) {
		String tempAttachmentsHtmlString = attachmentsHtml;
		for (final Reference attachment : attachments) {
			try {
				tempAttachmentsHtmlString += archiveAttachment(attachment, archiveId, siteId, subdirs, finalFolder, attachmentsHtml);
			} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
				log.error("Error getting attachment: " + attachment.getId());
			}
		}
		return tempAttachmentsHtmlString;
	}

	/**
	 * Helper method to archive an attachment
	 *
	 * @param attachment
	 * @param archiveId
	 * @param siteId
	 * @param subdir
	 * @param finalFolder
	 * @param attachmentsHtml
	 * @return attachmentsHtml
	 * @throws ServerOverloadException
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws TypeException
	 */
	private String archiveAttachment(final Reference attachment, final String archiveId, final String siteId, final String[] subdir,
			final String finalFolder, final String attachmentsHtml)
			throws ServerOverloadException, PermissionException, IdUnusedException, TypeException {
		final byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
		final String attachmentName = attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName());
		this.archiverService.archiveContent(archiveId, siteId, this.toolName, attachmentBytes, attachmentName,
				ArrayUtils.addAll(subdir, finalFolder));
		return addToAttachmentsHtml(finalFolder, attachmentName, attachmentsHtml);
	}

	/**
	 * Set the html string that contains a list of attachment hyperlinks
	 *
	 * @param attachmentLocation
	 * @param attachmentName
	 * @param attachmentsHtml
	 * @return attachmentsHtml
	 */
	private String addToAttachmentsHtml(final String attachmentLocation, final String attachmentName, final String attachmentsHtml) {

		final String sanitisedName = Sanitiser.sanitise(attachmentName);
		final String attachmentHyperlink = "<li><a href=\"./" + attachmentLocation + "/" + sanitisedName + "\">" + sanitisedName
				+ "</a></li>";
		return attachmentsHtml + attachmentHyperlink;
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
	private String[] getSubDirs(final Assignment assignment, final String submitterId) {

		final List<String> subDirs = new ArrayList<>();
		subDirs.add(assignment.getTitle());
		subDirs.add("submissions");

		// Get the user associated with this submitterId
		final User user = getUser(submitterId);
		if (user != null) {
			subDirs.add(user.getSortName());
			return subDirs.toArray(new String[subDirs.size()]);
		}

		// If a user wasn't found, maybe it's a group submission
		final Group group = getGroup(assignment.getContext(), submitterId);
		if (group != null) {
			subDirs.add(group.getTitle());
			return subDirs.toArray(new String[subDirs.size()]);
		}

		// Neither a user or group could be found, use submitterId as folder name
		log.error("Neither a user or group name could not be found for submitterId: {}", submitterId);
		subDirs.add(submitterId);
		return subDirs.toArray(new String[subDirs.size()]);

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
