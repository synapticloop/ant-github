package synapticloop.github.ant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("  !!! WARNING !!!");
		System.out.println("This jar file cannot be used as an executable - here is an ant buildfile to get you started.");

		InputStream resourceAsStream = Main.class.getResourceAsStream("/build-ant-github.xml");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
		String line = null;
		while((line = bufferedReader.readLine()) != null) {
			System.out.println(line);
		}
	}

}
