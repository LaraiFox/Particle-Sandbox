package net.laraifox.particlesandbox.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class FileUtils {
	private FileUtils() {
	}

	public static String readFile(File file) throws IOException {
		StringBuilder result = new StringBuilder();
		BufferedReader fileReader = new BufferedReader(new FileReader(file));
		
		String line = new String();
		while ((line = fileReader.readLine()) != null) {
			result.append(line).append("\n");
		}

		fileReader.close();

		return result.toString();
	}
}
