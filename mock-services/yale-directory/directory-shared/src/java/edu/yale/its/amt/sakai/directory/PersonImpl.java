/*
 * PeopleFilePersonImpl.java
 *
 * Created on April 5, 2006, 9:47 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.yale.its.amt.sakai.directory;

import java.io.Serializable;

/**
 * Mocked class
 */
@SuppressWarnings({ "unused", "serial" })
public class PersonImpl implements Person, Serializable {

	/** Creates a new instance of PeopleFilePersonImpl */
	public PersonImpl() {
	}

	/**
	 * Holds value of property firstName.
	 */
	private String firstName;

	/**
	 * Getter for property firstName.
	 *
	 * @return Value of property firstName.
	 */
	@Override
	public String getFirstName() {
		return this.firstName;
	}

	/**
	 * Setter for property firstName.
	 *
	 * @param firstName New value of property firstName.
	 */
	@Override
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Holds value of property middleName.
	 */
	private String middleName;

	/**
	 * Getter for property middleName.
	 *
	 * @return Value of property middleName.
	 */
	@Override
	public String getMiddleName() {
		return this.middleName;
	}

	/**
	 * Setter for property middleName.
	 *
	 * @param middleName New value of property middleName.
	 */
	@Override
	public void setMiddleName(final String middleName) {
		this.middleName = middleName;
	}

	/**
	 * Holds value of property lastName.
	 */
	private String lastName;

	/**
	 * Getter for property lastName.
	 *
	 * @return Value of property lastName.
	 */
	@Override
	public String getLastName() {
		return this.lastName;
	}

	/**
	 * Setter for property lastName.
	 *
	 * @param lastName New value of property lastName.
	 */
	@Override
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Holds value of property email.
	 */
	private String email;

	/**
	 * Getter for property email.
	 *
	 * @return Value of property email.
	 */
	@Override
	public String getEmail() {
		return this.email;
	}

	/**
	 * Setter for property email.
	 *
	 * @param email New value of property email.
	 */
	@Override
	public void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * Holds value of property peopleId.
	 */
	private String peopleId;

	/**
	 * Getter for property peopleId.
	 *
	 * @return Value of property peopleId.
	 */
	@Override
	public String getPeopleId() {
		return this.peopleId;
	}

	/**
	 * Setter for property peopleId.
	 *
	 * @param peopleId New value of property peopleId.
	 */
	@Override
	public void setPeopleId(final String peopleId) {
		this.peopleId = peopleId;
	}

	/**
	 * Holds value of property major.
	 */
	private String major;

	/**
	 * Getter for property major.
	 *
	 * @return Value of property major.
	 */
	@Override
	public String getMajor() {
		return this.major;
	}

	/**
	 * Setter for property major.
	 *
	 * @param major New value of property major.
	 */
	@Override
	public void setMajor(final String major) {
		this.major = major;
	}

	/**
	 * Holds value of property classYear.
	 */
	private String classYear;

	/**
	 * Getter for property classYear.
	 *
	 * @return Value of property classYear.
	 */
	@Override
	public String getClassYear() {
		return this.classYear;
	}

	/**
	 * Setter for property classYear.
	 *
	 * @param classYear New value of property classYear.
	 */
	@Override
	public void setClassYear(final String classYear) {
		this.classYear = classYear;
	}

	/**
	 * Holds value of property school.
	 */
	private String school;

	/**
	 * Getter for property school.
	 *
	 * @return Value of property school.
	 */
	@Override
	public String getSchool() {
		return this.school;
	}

	/**
	 * Setter for property school.
	 *
	 * @param school New value of property school.
	 */
	@Override
	public void setSchool(final String school) {
		this.school = school;
	}

	/**
	 * Holds value of property residentialCollege.
	 */
	private String residentialCollege;

	/**
	 * Getter for property residentialCollege.
	 *
	 * @return Value of property residentialCollege.
	 */
	@Override
	public String getResidentialCollege() {
		return this.residentialCollege;
	}

	/**
	 * Setter for property residentialCollege.
	 *
	 * @param residentialCollege New value of property residentialCollege.
	 */
	@Override
	public void setResidentialCollege(final String residentialCollege) {
		this.residentialCollege = residentialCollege;
	}

	/**
	 * Holds value of property personType.
	 */
	private String personType;

	/**
	 * Getter for property personType.
	 *
	 * @return Value of property personType.
	 */
	@Override
	public String getPersonType() {
		return this.personType;
	}

	/**
	 * Setter for property personType.
	 *
	 * @param personType New value of property personType.
	 */
	@Override
	public void setPersonType(final String personType) {
		this.personType = personType;
	}

	/**
	 * Holds value of property studentFlag.
	 */
	private String studentFlag;

	/**
	 * Getter for property studentFlag.
	 *
	 * @return Value of property studentFlag.
	 */
	@Override
	public String getStudentFlag() {
		return this.studentFlag;
	}

	/**
	 * Setter for property studentFlag.
	 *
	 * @param studentFlag New value of property studentFlag.
	 */
	@Override
	public void setStudentFlag(final String studentFlag) {
		this.studentFlag = studentFlag;
	}

	/**
	 * Holds value of property netid.
	 */
	private String netid;

	/**
	 * Getter for property netid.
	 *
	 * @return Value of property netid.
	 */
	@Override
	public String getNetid() {
		return this.netid;
	}

	/**
	 * Setter for property netid.
	 *
	 * @param netid New value of property netid.
	 */
	@Override
	public void setNetid(final String netid) {
		this.netid = netid;
	}

	/**
	 * Holds value of property bannerId.
	 */
	private String bannerId;

	/**
	 * Getter for property bannerId.
	 *
	 * @return Value of property bannerId.
	 */
	@Override
	public String getBannerId() {
		return this.bannerId;
	}

	/**
	 * Setter for property bannerId.
	 *
	 * @param bannerId New value of property bannerId.
	 */
	@Override
	public void setBannerId(final String bannerId) {
		this.bannerId = bannerId;
	}

	private String sid;

	/**
	 * Getter for property sId.
	 *
	 * @return Value of property sId.
	 */
	@Override
	public String getSid() {
		return this.sid;
	}

	/**
	 * Setter for property sid.
	 *
	 * @param sid New value of property studentId.
	 */
	@Override
	public void setSid(final String sid) {
		this.sid = sid;
	}

	@Override
	public int compareTo(final Object o) {
		final Person other = (Person) o;
		int retval = compareStrings(getLastName(), other.getLastName());
		if (retval != 0) {
			return retval;
		}
		retval = compareStrings(getFirstName(), other.getFirstName());
		if (retval != 0) {
			return retval;
		}
		retval = compareStrings(getMiddleName(), other.getMiddleName());
		if (retval != 0) {
			return retval;
		}
		return getNetid().compareTo(other.getNetid());
	}

	private int compareStrings(final String s1, final String s2) {
		if (s1 == null) {
			return 0;
		}
		if (s2 == null) {
			return 0;
		}
		return s1.compareTo(s2);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Person)) {
			return false;
		}
		final Person other = (Person) obj;
		return getNetid().equals(other.getNetid());
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
