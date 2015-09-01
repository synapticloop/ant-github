package synapticloop.github.ant;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.json.JSONArray;
import org.json.JSONObject;

import synapticloop.github.ant.util.HttpHelper;

public class GetReleaseTask extends Task {
	private String owner = null;
	private String repo = null;
	private String version = null;
	private String asset = null;
	private String out = null;

	@Override
	public void execute() throws BuildException {
		checkParameter("owner", owner);
		checkParameter("repo", repo);
		checkParameter("asset", asset);

		if(null == version || version.trim().length() == 0) {
			getProject().log("No version set, using version 'latest'");
			version = "latest";
		}

		//checkParameter("out", out);

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
						downloadableAssetUrl = assetObject.getString("browser_download_url");
						break;
					}
				}
			}

			if(null != downloadableAssetUrl) {
				File file = new File(out);
				HttpHelper.writeUrlToFile(downloadableAssetUrl, file);
			} else {
				throw new BuildException("Could not find a downloadable asset for '" + asset + "'.");
			}
		} catch (IOException ioex) {
			throw new BuildException("could not determine releases from '" + url + "'.", ioex);
		}
		super.execute();
	}

	private static void checkParameter(String name, String parameter) throws BuildException {
		if(null == parameter || parameter.trim().length() == 0) {
			throw new BuildException("Task parameter '" + name + "', was not provided, failing.");
		}
	}

	public void setOwner(String owner) { this.owner = owner; }
	public void setRepo(String repo) { this.repo = repo; }
	public void setVersion(String version) { this.version = version; }
	public void setOut(String out) { this.out = out; }
	public void setAsset(String asset) { this.asset = asset; }

}
