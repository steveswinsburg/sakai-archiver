/*
 * YaleDirectoryServiceImpl.java
 *
 * Created on April 5, 2006, 9:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.yale.its.amt.sakai.directory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Mocked class
 */
public class YaleDirectoryServiceImpl implements YaleDirectoryService {

	@Override
	public Person lookupPersonByNetid(final String netid) throws YaleDirectoryException {
		return new PersonImpl();
	}

	@Override
	public Person lookupPersonByNetid(final String netid, final boolean loadBannerData) throws YaleDirectoryException {
		return new PersonImpl();
	}

	@Override
	public Person newPerson() {
		return new PersonImpl();
	}

	@Override
	public List lookupPeopleByNetids(final Collection netids) throws YaleDirectoryException {
		return new ArrayList();
	}

	@Override
	public List lookupPeopleByNetids(final Collection netids, final boolean loadBannerData) throws YaleDirectoryException {
		return new ArrayList();
	}

	@Override
	public String lookupActualPeopleFileEmail(final String netid) throws YaleDirectoryException {
		return "";
	}

	@Override
	public String lookupEmailOverride(final String netid) throws YaleDirectoryException {
		return "";
	}

	@Override
	public void createEmailOverride(final String netid, final String email) throws YaleDirectoryException {
	}

	@Override
	public void updateEmailOverride(final String netid, final String email) throws YaleDirectoryException {
	}

	@Override
	public void deleteEmailOverride(final String netid) throws YaleDirectoryException {
	}

}
