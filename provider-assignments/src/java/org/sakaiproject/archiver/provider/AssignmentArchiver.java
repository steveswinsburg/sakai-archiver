package org.sakaiproject.archiver.provider;

import java.util.Collection;
import java.util.List;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.util.Jsonifier;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.model.AssignmentAllPurposeItem;
import org.sakaiproject.assignment.api.model.AssignmentModelAnswerItem;
import org.sakaiproject.assignment.api.model.AssignmentNoteItem;
import org.sakaiproject.assignment.api.model.AssignmentSupplementItemService;
import org.sakaiproject.content.api.ContentHostingService;
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
public class AssignmentArchiver implements Archiveable {

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

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {
		
		List<Assignment> assignments = this.assignmentService.getListAssignmentsForContext(siteId);

		for (Assignment assignment : assignments) {

			// archive the assignment data
			SimpleAssignment simpleAssignment = new SimpleAssignment(assignment);
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleAssignment).getBytes(), "details.json", assignment.getTitle());

			// archive the attachments for the assignment
			for (Reference attachment : assignment.getContent().getAttachments()) {
				try {
					archiveAttachments(attachment, archiveId, siteId, toolId, assignment.getTitle() + "/attachments");
				} catch (ServerOverloadException | IdUnusedException | TypeException | PermissionException e) {
					log.error("Error getting attachment for assignment: " + assignment.getTitle());
					continue;
				} 
			}
			
			// archive the grades spreadsheet for the assignment
//			byte[] gradesSpreadsheet;
//			try {
//				gradesSpreadsheet = this.assignmentService.getGradesSpreadsheet(this.assignmentService.gradesSpreadsheetReference(siteId, assignment.getId()));
//				if (gradesSpreadsheet != null ) {
//					this.archiverService.archiveContent(archiveId, siteId, toolId, gradesSpreadsheet, "grades.csv", assignment.getTitle());
//				}
//			} catch (IdUnusedException | PermissionException e) {
//				log.error("Error getting grades spreadsheet for assignment: " + assignment.getTitle());
//				continue;
//			}
			
			// if we want student content, archive the submissions for the assignment
			if (includeStudentContent) {
				this.archiveSubmissions(assignment, archiveId, siteId, toolId);
			}
		}
	}

	/**
	 * Get the submissions for an assignment, and any feedback from the instructor
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 */
	@SuppressWarnings("unchecked")
	private void archiveSubmissions(Assignment assignment, String archiveId, String siteId, String toolId) {
		List<AssignmentSubmission> submissions = this.assignmentService.getSubmissions(assignment);
		
		for (AssignmentSubmission submission : submissions) {
			
			// get the attachments for this submission
			for (Object submissionObj : submission.getSubmittedAttachments()) {
				String subdirs = getSubDirs(assignment, submission.getSubmitterId(), "submission", siteId);
				
				try {
					archiveAttachments((Reference)submissionObj, archiveId, siteId, toolId, subdirs);
				} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
					log.error("Error getting submission attachment for assignment: " + assignment.getTitle());
				}
				// get other data associated with this submission
				SimpleSubmission submissionData = new SimpleSubmission(submission, assignment.isGroup());
				this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(submissionData).getBytes(), "submission.json", subdirs);
			}

			// get the attachments provided by the instructor when grading the submission
			for (Object gradingObj : submission.getFeedbackAttachments()) {
				String subdirs = getSubDirs(assignment, submission.getSubmitterId(), "feedback", siteId);
				try {
					archiveAttachments((Reference)gradingObj, archiveId, siteId, toolId, subdirs);
				} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
					log.error("Error getting feedback attachment for assignment: " + assignment.getTitle());
				}
				// get other data associated with this feedback
				SimpleFeedback feedback = new SimpleFeedback(submission);
				this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(feedback).getBytes(), "feedback.json", subdirs);
			}
		}
	}
	
	
	/**
	 * Helper method to archive attachments
	 * 
	 * @param attachment
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 * @param title
	 * @throws ServerOverloadException
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws TypeException
	 */
	private void archiveAttachments(Reference attachment, String archiveId, String siteId, String toolId, String subdir) throws ServerOverloadException, PermissionException, IdUnusedException, TypeException {
		byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
		this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes, 
				attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName()), subdir);
	}

	/**
	 * Get the subdirectory structure
	 * @param assignmentTitle
	 * @param submitterId
	 * @param folderName
	 * @return
	 */
	private String getSubDirs(Assignment assignment, String submitterId, String folderName, String siteId) {

		try {
			if (assignment.isGroup()) {
				Group group = this.siteService.getSite(siteId).getGroup(submitterId);
				return assignment.getTitle() + "/" + group.getTitle() + "/" + folderName; 
			} else {
				User user = this.userDirectoryService.getUser(submitterId);
				return assignment.getTitle() + "/" + user.getLastName() + ", " + user.getFirstName() + "/" + folderName;
			}
		} catch (UserNotDefinedException e) {
			log.error("Unable to find user associated with assignment submission");
		} catch (IdUnusedException e) {
			log.error("Could not look up group for assignment");
		}
		return assignment.getTitle() + "/unknown/submission";
	}

	/**
	 * Get the gradebook item associated with an assignment.
	 * @param a the Assignment
	 * @param gradebookItem the SimpleGradebookItem
	 * @return the populated SimpleGradebookItem
	 */
	private SimpleGradebookItem getGradebookFields(Assignment a) {
		
		if (gradebookService.isGradebookDefined(a.getContext())) {
			SimpleGradebookItem gradebookItem = new SimpleGradebookItem();
			String gradebookAssignmentProp = a.getProperties().getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
			if (gradebookAssignmentProp != null) {
				org.sakaiproject.service.gradebook.shared.Assignment gAssignment = gradebookService.getAssignment(a.getContext(), gradebookAssignmentProp);
				if (gAssignment != null) {
					// there is a linked Gradebook item
					gradebookItem.setGradebookItemId(gAssignment.getId());
					gradebookItem.setGradebookItemName(gAssignment.getName());
					return gradebookItem;
				}
			}		
		}
		return null;
	}

	
	/**
	 * Simplified helper class to represent feedback for a submission
	 */
	private class SimpleFeedback {
		@Setter
		String feedbackComment;

		@Setter 
		String feedbackText;

		@Setter
		String gradedBy;
		
		public SimpleFeedback(AssignmentSubmission submission) {
			this.feedbackComment = submission.getFeedbackComment();
			this.feedbackText = submission.getFeedbackText();
			this.gradedBy = submission.getGradedBy();
		}
	}
	
	/**
	 * Simplified helper class to represent an individual submission for an assignment
	 */
	private class SimpleSubmission {

		@Setter 
		String status;

		@Setter
		String submittedText;

		@Setter
		String submitterId;

		@Setter
		List<String> groupSubmitterIds;

		@Setter
		String timeSubmitted;

		public SimpleSubmission(AssignmentSubmission submission, boolean group) {
			this.status = submission.getStatus();
			this.submittedText = submission.getSubmittedText();
			this.submitterId = submission.getSubmitterId();
			this.timeSubmitted = submission.getTimeSubmittedString();
			if (group) {
				this.groupSubmitterIds = submission.getSubmitterIds();
			}
		}
	}

	/**
	 * Simplified helper class to represent an individual assignment item in a site
	 */
	private class SimpleAssignment {

		@Setter
		private String id;

		@Setter
		private String timeOpen;

		@Setter
		private String timeDue;

		@Setter
		private String lateAfter;

		@Setter
		private String acceptUntil;

		@Setter
		private boolean draft;

		@Setter
		private String creator;

		@Setter
		private List authors;

		@Setter
		private String instructions;

		@Setter
		private String authorLastModified;

		@Setter
		private String title;

		@Setter
		private String status;

		@Setter
		private Collection groups;

		@Setter
		private String access;

		@Setter
		private String gradeScale;

		@Setter
		private String gradeScaleMaxPoints;

		@Setter
		private String submissionType;

		@Setter
		private String modelAnswerText;

		@Setter
		private String privateNoteText;

		@Setter
		private String allPurposeItemText;

		@Setter
		private SimpleGradebookItem gradebookItemDetails;

		public SimpleAssignment(Assignment a) {
			if (a == null){
				return;
			}

			// These fields can simply be copied over
			this.id = a.getId();
			this.timeOpen = a.getOpenTimeString();
			this.timeDue = a.getDueTimeString();
			this.lateAfter = a.getDropDeadTimeString();
			this.acceptUntil = a.getCloseTimeString();
			this.draft = a.getDraft();
			this.creator = a.getCreator();
			this.authors = a.getAuthors();
			this.authorLastModified = a.getAuthorLastModified();
			this.title = a.getTitle();
			this.status = a.getStatus();
			this.access = a.getAccess().toString();
			
			// See if there are groups submissions for this assignment
			if (a.isGroup()) {
				this.groups = a.getGroups();
			}

			// See if there is a gradebook item associated with this assignment
			SimpleGradebookItem gradebookItem = getGradebookFields(a);
			if (gradebookItem != null) {
				this.gradebookItemDetails = gradebookItem;
			}

			// Get content related fields
			if (a.getContent() != null) {
				this.instructions = a.getContent().getInstructions();
				this.gradeScale = a.getContent().getTypeOfGradeString();
				this.submissionType = a.getContent().getTypeOfSubmissionString();

				//if grade scale is "points", get the maximum points allowed
				this.gradeScaleMaxPoints = a.getContent().getMaxGradePointDisplay();
			}

			// Supplement Items
			AssignmentModelAnswerItem assignmentModelAnswerItem = assignmentSupplementItemService.getModelAnswer(a.getId());
			if (assignmentModelAnswerItem != null) {
				this.modelAnswerText = assignmentModelAnswerItem.getText();
			}
			AssignmentNoteItem assignmentNoteItem = assignmentSupplementItemService.getNoteItem(a.getId());
			if (assignmentNoteItem != null) {
				this.privateNoteText = assignmentNoteItem.getNote();
			}
			AssignmentAllPurposeItem assignmentAllPurposeItem = assignmentSupplementItemService.getAllPurposeItem(a.getId());
			if (assignmentAllPurposeItem != null) {
				this.allPurposeItemText =  assignmentAllPurposeItem.getText();
			}
		}
	}

	/**
	 * Simplified helper class for a gradebook item
	 */
	private class SimpleGradebookItem {

		@Setter
		private Long gradebookItemId;

		@Setter
		private String gradebookItemName;
	}
}
