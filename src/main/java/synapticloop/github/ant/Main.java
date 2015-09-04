package synapticloop.github.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Main {

	private static final String BUILD_ANT_GITHUB_XML = "build-ant-github.xml";

	public static void main(String[] args) throws IOException {
		InputStream resourceAsStream = Main.class.getResourceAsStream("/" + BUILD_ANT_GITHUB_XML);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));

		if(args.length != 0 && "example".equalsIgnoreCase(args[0])) {
			writeToFile(bufferedReader);
		} else {
			writeToOutput(bufferedReader);
		}

		bufferedReader.close();
	}

	private static void writeToOutput(BufferedReader bufferedReader) throws IOException {
		System.out.println("  !!! WARNING !!!");
		System.out.println("This jar file cannot be used as an executable - here is an ant buildfile to get you started.");

		String line = null;
		while((line = bufferedReader.readLine()) != null) {
			System.out.println(line);
		}

		System.out.println("\n\nNOTE: If you run this as ");
		System.out.println("\t java -jar ant-github.jar example");
		System.out.println("an example build file will be written.");
	}


	private static void writeToFile(BufferedReader bufferedReader) throws IOException {
		File file = new File(BUILD_ANT_GITHUB_XML);
		if(file.exists()) {
			System.out.println("File '" + BUILD_ANT_GITHUB_XML + "' already exists, ignoring...");
			return;
		}

		FileWriter fileWriter = new FileWriter(file);
		String line = null;
		while((line = bufferedReader.readLine()) != null) {
			fileWriter.write(line);
		}

		fileWriter.flush();
		fileWriter.close();
		System.out.println("Example build file '" + BUILD_ANT_GITHUB_XML + "' written to the file system.");

	}

}
