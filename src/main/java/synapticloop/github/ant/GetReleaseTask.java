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
	private static final String BROWSER_DOWNLOAD_URL = "browser_download_url";

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

	@Override
	public void execute() throws BuildException {
		checkParameter("owner", owner);
		checkParameter("repo", repo);
		checkParameter("asset", asset);

		if(null == version || version.trim().length() == 0) {
			getProject().log(this, "No version set, using version 'latest'", Project.MSG_INFO);
			version = "latest";
		} else {
			// we need to get the version from the tagged release
			version = "tags/" + version;
		}

		checkParameter("outDir", outDir);

		String url = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/" + version;

		String downloadableAssetUrl = null;

		try {
			JSONObject jsonObject = new JSONObject(HttpHelper.getUrlContents(url));
			JSONArray jsonArray = jsonObject.getJSONArray("assets");
			if(jsonArray.length() == 0) {
				throw new BuildException("There were no assets in the release version '" + version + "'");
			} else {
				for(int i = 0; i < jsonArray.length(); i++) {
					JSONObject assetObject = jsonArray.getJSONObject(i);
					String name = assetObject.getString("name");
					if(name.equals(asset)) {
						// this is the one we want
						downloadableAssetUrl = assetObject.getString(BROWSER_DOWNLOAD_URL);
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
						getProject().log(this, "Created missing output directory of '" + outputDirectory.getPath() + "'.", Project.MSG_INFO);
					} else {
						logAndThrow("Could not create missing output directory of '" + outputDirectory.getPath() + "'.");
					}
				}

				if(outputDirectory.exists() && outputDirectory.isFile()) {
					logAndThrow("Output directory '" + outputDirectory.getPath() + "', exists, but is not a directory, please remove this file.");
				}

				File outputFile = new File(outputDirectory.getPath() + File.separatorChar+ asset);
				if(outputFile.exists()) {
					logAndThrow("File '" + outputFile.getName() + "' already exists, please delete this file or use the overwrite=\"true\" attribute on this task.");
				}
				HttpHelper.writeUrlToFile(downloadableAssetUrl, outputFile);
				getProject().log(this, "Successfully downloaded release " + owner + "/" + repo + "/" + version + "/" + asset + " -> " + outputFile.getPath(), Project.MSG_INFO);
			} else {
				throw new BuildException("Could not find a downloadable asset for '" + asset + "'.");
			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
			throw new BuildException("Could not determine releases from '" + url + "'.", ioex);
		}
	}

	private void checkParameter(String name, String parameter) throws BuildException {
		if(null == parameter || parameter.trim().length() == 0) {
			logAndThrow("Task parameter '" + name + "', was not provided, failing...");
		}
	}

	private void logAndThrow(String message) throws BuildException {
		getProject().log(this, message, Project.MSG_ERR);
		throw new BuildException(message);
	}

	public void setOwner(String owner) { this.owner = owner; }
	public void setRepo(String repo) { this.repo = repo; }
	public void setVersion(String version) { this.version = version; }
	public void setOutDir(String outDir) { this.outDir = outDir; }
	public void setAsset(String asset) { this.asset = asset; }

}
