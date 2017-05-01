package org.sakaiproject.archiver.persistence;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.impl.ArchiveMapper;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

public class ArchiverPersistenceService extends HibernateDaoSupport {

	
	public void init() {
		
	}
	
	public void destroy() {
		
	}
	
	/**
	 * Create a new archive
	 * 
	 * @param siteId
	 * @param userUuid
	 * @return
	 */
	public Archive create(String siteId, String userUuid) {
	
	
	    ArchiveEntity entity = new ArchiveEntity();
	    entity.setSiteId(siteId);
	    entity.setUserUuid(userUuid);
	    entity.setStartDate(new Date());
	    save(entity); 
	    
	    System.out.println("saved: " + entity.getId());
	    
	    return ArchiveMapper.toDto(entity);
	   
	}
	
	/**
	 * Get a current archive for the given site. Return null if none exists or is not currently active.
	 * 
	 * @param siteId
	 * @param userUuid
	 * @return
	 */
	public Archive getCurrent(String siteId) {
		
		Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria(ArchiveEntity.class);
		criteria.add(Restrictions.eq("siteId", siteId));
		criteria.add(Restrictions.isNull("endDate"));
		// TODO catch exception if there is more than one for whatever reason, and deal with it.
		ArchiveEntity entity = (ArchiveEntity) criteria.uniqueResult();
		
	    return ArchiveMapper.toDto(entity);
	}
	
	/**
	 * Helper to save some sort of entity to the database
	 * @param entity
	 */
	private <T> void save(T entity) {
		Session session = this.getSessionFactory().getCurrentSession();
	    session.beginTransaction();
	    session.save(entity); 
	    session.getTransaction().commit();
	    session.close();
	}
	
	
}
