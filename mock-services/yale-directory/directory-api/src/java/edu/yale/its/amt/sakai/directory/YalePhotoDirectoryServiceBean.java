package edu.yale.its.amt.sakai.directory;

import org.sakaiproject.component.cover.ComponentManager;


public class YalePhotoDirectoryServiceBean {
	
	
	private static YalePhotoDirectoryService m_instance = null;

	public static YalePhotoDirectoryService getInstance() {
		if (ComponentManager.CACHE_COMPONENTS){
			if (m_instance == null)
				m_instance = (YalePhotoDirectoryService) ComponentManager.get("edu.yale.its.amt.sakai.directory.YalePhotoDirectoryService");
			
			return m_instance;
		}
		else
		{
			return (YalePhotoDirectoryService) ComponentManager.get("edu.yale.its.amt.sakai.directory.YalePhotoDirectoryService");
		}
	}
	
	public static boolean isShowMyPhotoStatus(String netId) throws YalePhotoDirectoryServiceException
	{
		YalePhotoDirectoryService service = getInstance();
		if (service == null) return false;

		return service.isShowMyPhotoStatus(netId);
	}

	public static void setShowMyPhotoStatus(boolean showStatus, String netId) throws YalePhotoDirectoryServiceException {
		YalePhotoDirectoryService service = getInstance();
		if (service == null) 
			return;
		
		service.setShowMyPhotoStatus(showStatus,netId);
	}

	public static boolean isShowPublicViewPhotoOption(String netId) throws YalePhotoDirectoryServiceException {
		YalePhotoDirectoryService service = getInstance();
		if (service == null) 
			return false;
		
		return service.isShowPublicViewPhotoOption(netId);
	}

}
