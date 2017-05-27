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
	private ContentHostingService contentHostingService;

	@Setter
	private AssignmentService assignmentService;

	@Setter
	private ArchiverService archiverService;
	
	@Setter
	private AssignmentSupplementItemService assignmentSupplementItemService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		List<Assignment> assignments = this.assignmentService.getListAssignmentsForContext(siteId);

		for (Assignment assignment : assignments) {
			
			// get the assignment data
			SimpleAssignment simpleAssignment = new SimpleAssignment(assignment);
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleAssignment).getBytes(), assignment.getTitle() + ".json");
			
			// get the attachments for the assignment
			for (Reference attachment : assignment.getContent().getAttachments()) {
				byte[] attachmentBytes;
				try {
					attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
					this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes, 
							attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName()), 
							assignment.getTitle() + " (attachments)");
				} catch (ServerOverloadException | IdUnusedException | TypeException | PermissionException e) {
					log.error("Error getting attachment for assignment: " + assignment.getTitle());
					continue;
				} 
			}
		}
	}

	/**
	 * Simplified helper class to represent an individual assignment item in a site
	 */
	public class SimpleAssignment {

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
			this.section = a.getSection();
			this.draft = a.getDraft();
			this.creator = a.getCreator();
			this.timeCreated = a.getTimeCreated().toString();
			this.authors = a.getAuthors();
			this.lastModified = a.getTimeLastModified().toString();
			this.authorLastModified = a.getAuthorLastModified();
			this.title = a.getTitle();
			this.status = a.getStatus();
			this.groups = a.getGroups();
			this.access = a.getAccess().toString();
			
			// See if there is a gradebook item associated with this assignment
			if (gradebookService.isGradebookDefined(a.getContext())) {
				this.gradebookItemDetails = getGradebookFields(a);
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

	/**
	 * Get the gradebook item associated with an assignment.
	 * @param a the Assignment
	 * @param gradebookItem the SimpleGradebookItem
	 * @return the populated SimpleGradebookItem
	 */
	private SimpleGradebookItem getGradebookFields(Assignment a) {
		SimpleGradebookItem gradebookItem = new SimpleGradebookItem();
		String gradebookAssignmentProp = a.getProperties().getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
		if (gradebookAssignmentProp != null) {
			org.sakaiproject.service.gradebook.shared.Assignment gAssignment = gradebookService.getAssignment(a.getContext(), gradebookAssignmentProp);
			if (gAssignment != null) {
				// linked Gradebook item is internal
				gradebookItem.setGradebookItemId(gAssignment.getId());
				gradebookItem.setGradebookItemName(gAssignment.getName());
			} else {
				log.info("Gradebook item could not be found for assignment " + a.getTitle() + ". It could be linked to an external gradebook.");
			}
		}		
		return gradebookItem;
	}
}
