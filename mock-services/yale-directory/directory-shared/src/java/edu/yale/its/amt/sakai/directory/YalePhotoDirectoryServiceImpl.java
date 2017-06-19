package edu.yale.its.amt.sakai.directory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
		final byte[] bytes = null;
		final File sampleJpg = new File(getClass().getResource("/photo.jpg").getFile());
		try {
			final InputStream is = FileUtils.openInputStream(sampleJpg);
			IOUtils.readFully(is, bytes);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return bytes;

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
