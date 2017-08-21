package edu.yale.its.amt.sakai.directory;

import java.util.Collection;
import org.sakaiproject.site.api.Site;

public interface YalePhotoDirectoryService {

	public static String CLEAR_CACHE_PERMISSION = "roster.clear_cache";

	public void clearCache();

	 public boolean checkYalePhotoClearCachePermission(String permission);

	boolean isPrimaryInstructor(String current_user_eid, Site site) throws YalePhotoDirectoryServiceException, Exception;

	byte[] loadPhotoFromCache(String netId) throws YalePhotoDirectoryServiceException;

	void loadPhotos(Collection<String> netIds) throws YalePhotoDirectoryServiceException;

	void loadPhotos(Collection<String> netIds, String siteId) throws YalePhotoDirectoryServiceException;

	public boolean isShowMyPhotoStatus(String netId) throws YalePhotoDirectoryServiceException;

	public void setShowMyPhotoStatus(boolean showStatus, String netId) throws YalePhotoDirectoryServiceException;

	public boolean isShowPublicViewPhotoOption(String netId) throws YalePhotoDirectoryServiceException;

	public String md5CheckSum(String netId);

}
