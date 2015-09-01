package synapticloop.github.ant.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {
	public static String getUrlContents(String url) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}

		in.close();

		return(response.toString());

	}

	public static void writeUrlToFile(String url, File out) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		InputStream inputStream = con.getInputStream();
		OutputStream outputStream = new FileOutputStream(out);
		byte[] buffer = new byte[4096];
		int n = 0;
		while ((n = inputStream.read( buffer )) != -1) {
			outputStream.write( buffer, 0, n );
		}

		outputStream.close();
	}
}
