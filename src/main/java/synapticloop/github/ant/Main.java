package synapticloop.github.ant;


public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GetReleaseTask getReleaseTask = new GetReleaseTask();
		getReleaseTask.setOwner("synapticloop");
		getReleaseTask.setRepo("JSON-java");
		getReleaseTask.setVersion("latest");
		getReleaseTask.setOut("lib/runtime/JSON-java.jar");
		getReleaseTask.setAsset("JSON-java.jar");
		getReleaseTask.execute();
	}

}
