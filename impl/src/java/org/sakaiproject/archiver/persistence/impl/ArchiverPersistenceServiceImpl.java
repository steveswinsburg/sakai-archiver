package org.sakaiproject.archiver.persistence.impl;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverPersistenceServiceImpl extends HibernateDaoSupport implements ArchiverPersistenceService {

	public void init() {

	}

	public void destroy() {

	}

	@Override
	public ArchiveEntity create(final String siteId, final String userUuid) {

		final ArchiveEntity entity = new ArchiveEntity();
		entity.setSiteId(siteId);
		entity.setUserUuid(userUuid);
		entity.setStartDate(new Date());
		entity.setStatus(Status.STARTED);

		final Session session = getSessionFactory().getCurrentSession();
		session.save(entity);
		session.flush();

		log.debug("saved: " + entity.getId());

		return entity;
	}

	@Override
	public ArchiveEntity update(final ArchiveEntity entity) {

		final Session session = getSessionFactory().getCurrentSession();
		session.update(entity);
		session.flush();

		log.debug("saved: " + entity.getId());
		return entity;
	}

	@Override
	public ArchiveEntity getLatest(final String siteId) {
		final Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(ArchiveEntity.class);
		criteria.add(Restrictions.eq("siteId", siteId));
		criteria.addOrder(Order.desc("endDate"));
		criteria.setMaxResults(1);
		return (ArchiveEntity) criteria.uniqueResult();
	}

	@Override
	public ArchiveEntity getByArchiveId(final String archiveId) {
		final Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(ArchiveEntity.class);
		criteria.add(Restrictions.eq("id", archiveId));
		return (ArchiveEntity) criteria.uniqueResult();
	}

	@Override
	public List<ArchiveEntity> getBySiteId(final String siteId) {
		final Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(ArchiveEntity.class);
		criteria.add(Restrictions.eq("siteId", siteId));
		criteria.addOrder(Order.desc("startDate"));
		criteria.addOrder(Order.desc("endDate"));
		return criteria.list();
	}

}
