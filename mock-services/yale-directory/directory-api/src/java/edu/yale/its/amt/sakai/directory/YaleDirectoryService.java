/*
 * YaleDirectoryService.java
 *
 * Created on April 5, 2006, 9:39 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.yale.its.amt.sakai.directory;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author mikea
 */
public interface YaleDirectoryService
{
    /*
     *  Banner and people file data
     */
    Person lookupPersonByNetid(String netid)  throws YaleDirectoryException;
    Person lookupPersonByNetid(String netid, boolean loadBannerData)  throws YaleDirectoryException;
    Person newPerson();
    List lookupPeopleByNetids(Collection netids)  throws YaleDirectoryException;
    List lookupPeopleByNetids(Collection netids, boolean loadBannerData)  throws YaleDirectoryException;
    
    /*
     *  People file email
     */
    String lookupActualPeopleFileEmail(String netid) throws YaleDirectoryException;
    String lookupEmailOverride(String netid) throws YaleDirectoryException;
    void createEmailOverride(String netid, String email) throws YaleDirectoryException;
    void updateEmailOverride(String netid, String email) throws YaleDirectoryException;
    void deleteEmailOverride(String netid) throws YaleDirectoryException;
}
