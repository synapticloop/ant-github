package synapticloop.github.ant;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.json.JSONArray;
import org.json.JSONObject;

import synapticloop.github.ant.util.HttpHelper;

public class GetReleaseTask extends Task {

	private static final String GITHUB_API_REPOS = "https://api.github.com/repos/";

	private static final String JSON_KEY_BROWSER_DOWNLOAD_URL = "browser_download_url";
	private static final String JSON_KEY_NAME = "name";
	private static final String JSON_KEY_ASSETS = "assets";

	// the owner of the github reporitories
	private String owner = null;
	// the repository name
	private String repo = null;
	// the version - if not set - will default to 'latest'
	private String version = null;
	// the name of the asset that you want to download
	private String asset = null;
	// the output directory
	private String outDir = null;
	// whether to over-write the file
	private boolean overwrite = false;

	// the shortname for logging
	private String details = null;

	// whether we are using the latest tag
	private boolean useLatest = false;

	/**
	 * List all of the releases for a file - marking out the version that will be 
	 * downloaded, whether it is a draft and/or a pre-release
	 */
	private void listReleases() {
		String url = GITHUB_API_REPOS + owner + "/" + repo + "/releases";

		try {
			String urlContents = HttpHelper.getUrlContents(url);

			JSONArray jsonArray = new JSONArray(urlContents);

			int releasesLength = jsonArray.length();

			int maxPrintReleases = 5;
			// only list the last 5 releases
			if(releasesLength <= 5) {
				maxPrintReleases = releasesLength;
			}

			getProject().log(this, "[ " + releasesLength + " ] release" + ((releasesLength != 1)? "s" : "") + " found in GitHub repository '" + owner + "/" + repo + "'.", Project.MSG_ERR);


			boolean isFirstAsset = true;
			for (int i = 0; i < releasesLength; i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String releaseNumber = jsonObject.getString("tag_name");
				boolean isDraft = jsonObject.getBoolean("draft");
				boolean isPreRelease = jsonObject.getBoolean("prerelease");

				JSONArray assetsArray = jsonObject.getJSONArray(JSON_KEY_ASSETS);

				for(int j = 0; j < assetsArray.length(); j++) {
					JSONObject assetObject = assetsArray.getJSONObject(j);
					String name = assetObject.getString(JSON_KEY_NAME);

					if(name.equals(asset)) {
						// this is the one we want

						// is this the requested asset (for non-'latest') releases
						boolean thisRelease = version.endsWith(releaseNumber);

						if(i < maxPrintReleases) {
							logVersionInfo(isFirstAsset, releaseNumber, isDraft, isPreRelease, thisRelease);
						} else if(thisRelease) {
							getProject().log(this, "    ...", Project.MSG_ERR);

							logVersionInfo(isFirstAsset, releaseNumber, isDraft, isPreRelease, thisRelease);
						}


						if(!isDraft && !isPreRelease) {
							isFirstAsset = false;
						}
						break;
					}
				}
			}

		} catch (IOException ioex) {
			throw new BuildException("Could not list releases from '" + url + "'.", ioex);
		}
	}

	private void logVersionInfo(boolean isFirstAsset, String releaseNumber, boolean isDraft, boolean isPreRelease, boolean thisRelease) {
		getProject().log(this, 
				"    version: " + 
				releaseNumber + ((isDraft)? " [ DRAFT ]" : "") + 
				((isPreRelease)? " [ PRERELEASE ]" : "") +  
				((isFirstAsset && !isDraft && !isPreRelease)? " <-- [ LATEST ]" : "") + 
				((useLatest && isFirstAsset)? " [ REQUESTED ] ": "") + 
				((!isFirstAsset && thisRelease)? " <-- [ REQUESTED ] ": ""), 
				Project.MSG_ERR);
	}

	@Override
	public void execute() throws BuildException {
		checkParameter("owner", owner);
		checkParameter("repo", repo);
		checkParameter("asset", asset);

		if(null == version || version.trim().length() == 0 || "latest".equals(version)) {
			getProject().log(this, "[ " + owner + "/" + repo + " " + asset + " ] No version set, using version 'latest'", Project.MSG_INFO);
			version = "latest";
			useLatest = true;
		} else {
			// we need to get the version from the tagged release
			version = "tags/" + version;
		}

		listReleases();

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("[ ");
		stringBuilder.append(owner);
		stringBuilder.append("/");
		stringBuilder.append(repo);
		stringBuilder.append(" ");
		stringBuilder.append(asset);
		stringBuilder.append("@");
		stringBuilder.append(version);
		stringBuilder.append(" ] ");

		details = stringBuilder.toString();


		checkParameter("outDir", outDir);

		String url = GITHUB_API_REPOS + owner + "/" + repo + "/releases/" + version;

		String downloadableAssetUrl = null;

		try {
			JSONObject jsonObject = new JSONObject(HttpHelper.getUrlContents(url));
			JSONArray jsonArray = jsonObject.getJSONArray(JSON_KEY_ASSETS);
			if(jsonArray.length() == 0) {
				throw new BuildException("There were no assets in the release version '" + version + "'");
			} else {
				for(int i = 0; i < jsonArray.length(); i++) {
					JSONObject assetObject = jsonArray.getJSONObject(i);
					String name = assetObject.getString(JSON_KEY_NAME);
					if(name.equals(asset)) {
						// this is the one we want
						downloadableAssetUrl = assetObject.getString(JSON_KEY_BROWSER_DOWNLOAD_URL);
						break;
					}
				}
			}

			if(null != downloadableAssetUrl) {
				File outputDirectory = new File(outDir);
				// ensure that the directory exists
				if(!outputDirectory.exists()) {
					// create the directories
					boolean mkdirs = outputDirectory.mkdirs();
					if(mkdirs) {
						getProject().log(this, details + "Created missing output directory of '" + outputDirectory.getPath() + "'.", Project.MSG_INFO);
					} else {
						logAndThrow("Could not create missing output directory of '" + outputDirectory.getPath() + "'.");
					}
				}

				if(outputDirectory.exists() && outputDirectory.isFile()) {
					logAndThrow("Output directory '" + outputDirectory.getPath() + "', exists, but is not a directory, please remove this file.");
				}

				File outputFile = new File(outputDirectory.getPath() + File.separatorChar + asset);
				if(outputFile.exists() && !overwrite) {
					getProject().log(this, "File '" + outputFile.getName() + "' already exists, please delete this file or use the overwrite=\"true\" attribute on this task.", Project.MSG_WARN);
				} else {
					HttpHelper.writeUrlToFile(downloadableAssetUrl, outputFile);
					getProject().log(this, details + "Successfully downloaded release -> " + outputFile.getPath(), Project.MSG_INFO);
				}
			} else {
				throw new BuildException("Could not find a downloadable asset for '" + asset + "'.");
			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
			throw new BuildException("Could not determine releases from '" + url + "'.", ioex);
		}
	}

	/**
	 * Check that a parameter is not null and not an empty string
	 * 
	 * @param name the parameter name to check (used for help message)
	 * @param parameter the parameter value to be checked
	 * 
	 * @throws BuildException if the parameter value is not correct
	 */
	private void checkParameter(String name, String parameter) throws BuildException {
		if(null == parameter || parameter.trim().length() == 0) {
			logAndThrow("Task parameter '" + name + "', was not provided, failing...");
		}
	}

	/**
	 * Log a message and throw a BuildException
	 * 
	 * @param message the message to output
	 * 
	 * @throws BuildException the build exception
	 */
	private void logAndThrow(String message) throws BuildException {
		getProject().log(this, details +  message, Project.MSG_ERR);
		throw new BuildException(message);
	}

	public void setOwner(String owner) { this.owner = owner; }
	public void setRepo(String repo) { this.repo = repo; }
	public void setVersion(String version) { this.version = version; }
	public void setOutDir(String outDir) { this.outDir = outDir; }
	public void setAsset(String asset) { this.asset = asset; }
	public void setOverwrite(boolean overwrite) { this.overwrite = overwrite; }
}
