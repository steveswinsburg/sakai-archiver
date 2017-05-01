package org.sakaiproject.archiver.impl;

import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;

/**
 * Maps entity to DTOs and vice versa
 * 
 * @since 12.0
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class ArchiveMapper {

	/**
	 * Map an {@link ArchiveEntity} to a {@Archive} dto or null if entity is null
	 * @param entity
	 * @return
	 */
	public static Archive toDto(ArchiveEntity entity){
		
		if (entity == null) {
			return null;
		}
		
		Archive dto = new Archive();
		dto.setSiteId(entity.getSiteId());
		dto.setUserUuid(entity.getUserUuid());
		dto.setStartDate(entity.getStartDate());
		dto.setEndDate(entity.getEndDate());
		return dto;
	}
}
