package org.sakaiproject.archiver.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

	private ArchiveMapper() {
	}

	/**
	 * Map an {@link ArchiveEntity} to a {@link Archive} dto or null if entity is null
	 *
	 * @param entity
	 * @return
	 */
	public static Archive toDto(final ArchiveEntity entity) {

		if (entity == null) {
			return null;
		}

		final Archive dto = new Archive();
		dto.setArchiveId(entity.getId());
		dto.setSiteId(entity.getSiteId());
		dto.setUserUuid(entity.getUserUuid());
		dto.setStartDate(entity.getStartDate());
		dto.setEndDate(entity.getEndDate());
		dto.setStatus(entity.getStatus());
		dto.setZipPath(entity.getZipPath());

		return dto;
	}

	/**
	 * Map a list of {@link ArchiveEntity} to a list of {@link Archive} dto
	 *
	 * @param entities
	 * @return
	 */
	public static List<Archive> toDtos(final List<ArchiveEntity> entities) {
		final List<Archive> archives = new ArrayList<>();
		archives.addAll(entities.stream().map(e -> ArchiveMapper.toDto(e)).collect(Collectors.toList()));
		return archives;
	}

}
