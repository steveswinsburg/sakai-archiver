package edu.yale.its.amt.sakai.directory;

import java.util.Base64;
import java.util.Collection;

import org.sakaiproject.site.api.Site;

public class YalePhotoDirectoryServiceImpl implements YalePhotoDirectoryService {

	@Override
	public boolean isPrimaryInstructor(final String netId, final Site site)
			throws Exception {
		return true;
	}

	@Override
	public byte[] loadPhotoFromCache(final String netId)
			throws YalePhotoDirectoryServiceException {

		final String base64Image = "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"
				+ "UhllDxSJ0oAnB4oHSgD//2Q==";

		return Base64.getDecoder().decode(base64Image);

	}

	@Override
	public void loadPhotos(final Collection<String> netIds)
			throws YalePhotoDirectoryServiceException {

	}

	@Override
	public boolean checkYalePhotoClearCachePermission(final String permission) {
		return true;
	}

	@Override
	public void clearCache() {
	}

	@Override
	public boolean isShowMyPhotoStatus(final String netId)
			throws YalePhotoDirectoryServiceException {
		return true;
	}

	@Override
	public void setShowMyPhotoStatus(final boolean showStatus, final String netId)
			throws YalePhotoDirectoryServiceException {
	}

	@Override
	public boolean isShowPublicViewPhotoOption(final String netId) throws YalePhotoDirectoryServiceException {
		return true;
	}

	@Override
	public String md5CheckSum(final String netId) {
		return "";
	}

}
