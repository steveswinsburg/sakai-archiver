/*
 * PeopleFilePerson.java
 *
 * Created on April 5, 2006, 9:37 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.yale.its.amt.sakai.directory;

import java.io.Serializable;

/**
 *
 * @author mikea
 */
public interface Person extends Serializable, Comparable
{
    /**
     * Getter for property firstName.
     * @return Value of property firstName.
     */
    public String getFirstName();

    /**
     * Setter for property firstName.
     * @param firstName New value of property firstName.
     */
    public void setFirstName(String firstName);

    /**
     * Getter for property lastName.
     * @return Value of property lastName.
     */
    public String getLastName();

    /**
     * Setter for property lastName.
     * @param lastName New value of property lastName.
     */
    public void setLastName(String lastName);

    /**
     * Getter for property netid.
     * @return Value of property netid.
     */
    public String getNetid();

    /**
     * Setter for property netid.
     * @param netid New value of property netid.
     */
    public void setNetid(String netid);

    /**
     * Getter for property classYear.
     * @return Value of property classYear.
     */
    public String getClassYear();

    /**
     * Setter for property classYear.
     * @param classYear New value of property classYear.
     */
    public void setClassYear(String classYear);

    /**
     * Getter for property major.
     * @return Value of property major.
     */
    public String getMajor();

    /**
     * Setter for property major.
     * @param major New value of property major.
     */
    public void setMajor(String major);

    /**
     * Getter for property school.
     * @return Value of property school.
     */
    public String getSchool();

    /**
     * Setter for property school.
     * @param school New value of property school.
     */
    public void setSchool(String school);

    /**
     * Getter for property middleName.
     * @return Value of property middleName.
     */
    public String getMiddleName();

    /**
     * Setter for property middleName.
     * @param middleName New value of property middleName.
     */
    public void setMiddleName(String middleName);

    /**
     * Getter for property peopleId.
     * @return Value of property peopleId.
     */
    public String getPeopleId();

    /**
     * Setter for property peopleId.
     * @param peopleId New value of property peopleId.
     */
    public void setPeopleId(String peopleId);

    /**
     * Getter for property residentialCollege.
     * @return Value of property residentialCollege.
     */
    public String getResidentialCollege();

    /**
     * Setter for property residentialCollege.
     * @param residentialCollege New value of property residentialCollege.
     */
    public void setResidentialCollege(String residentialCollege);

    /**
     * Getter for property personType.
     * @return Value of property personType.
     */
    public String getPersonType();

    /**
     * Setter for property personType.
     * @param personType New value of property personType.
     */
    public void setPersonType(String personType);

    /**
     * Getter for property studentFlag.
     * @return Value of property studentFlag.
     */
    public String getStudentFlag();

    /**
     * Setter for property studentFlag.
     * @param studentFlag New value of property studentFlag.
     */
    public void setStudentFlag(String studentFlag);

    /**
     * Getter for property email.
     * @return Value of property email.
     */
    public String getEmail();

    /**
     * Setter for property email.
     * @param email New value of property email.
     */
    public void setEmail(String email);

    /**
     * Getter for property bannerId.
     * @return Value of property bannerId.
     */
    public String getBannerId();

    /**
     * Setter for property bannerId.
     * @param bannerId New value of property bannerId.
     */
    public void setBannerId(String bannerId);

    /**
     * Getter for property sId.
     * @return Value of property sId.
     */
    public String getSid();
    /**
     * Setter for property sid.
     * @param sid New value of property studentId.
     */
    public void setSid(String sid);
    
}
