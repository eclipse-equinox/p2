package org.eclipse.equinox.internal.simpleconfigurator.manipulator;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;

public class SimpleConfiguratorManipulatorUtils {

	public static void writeConfiguration(BundleInfo[] simpleInfos, File outputFile) throws IOException {

		// if empty remove the configuration file
		if (simpleInfos == null || simpleInfos.length == 0) {
			if (outputFile.exists()) {
				outputFile.delete();
			}
			File parentDir = outputFile.getParentFile();
			if (parentDir.exists()) {
				parentDir.delete();
			}
			return;
		}

		// sort by symbolic name
		Arrays.sort(simpleInfos, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (o1 instanceof BundleInfo && o2 instanceof BundleInfo) {
					return ((BundleInfo) o1).getSymbolicName().compareTo(((BundleInfo) o2).getSymbolicName());
				}
				return 0;
			}
		});

		Utils.createParentDir(outputFile);
		BufferedWriter writer = null;
		IOException caughtException = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			for (int i = 0; i < simpleInfos.length; i++) {
				writeBundleInfoLine(simpleInfos[i], writer);
			}
		} catch (IOException e) {
			caughtException = e;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// we want to avoid over-writing the original exception
					if (caughtException != null)
						caughtException = e;
				}
			}
		}
		if (caughtException != null)
			throw caughtException;
	}

	public static void writeBundleInfoLine(BundleInfo bundleInfo, BufferedWriter writer) throws IOException {
		// symbolicName,version,location,startLevel,markedAsStarted
		StringBuffer buffer = new StringBuffer();
		buffer.append(bundleInfo.getSymbolicName());
		buffer.append(',');
		buffer.append(bundleInfo.getVersion());
		buffer.append(',');
		buffer.append(stringifyLocation(bundleInfo.getLocation()));
		buffer.append(',');
		buffer.append(bundleInfo.getStartLevel());
		buffer.append(',');
		buffer.append(bundleInfo.isMarkedAsStarted());
		writer.write(buffer.toString());
		writer.newLine();
	}

	public static String stringifyLocation(URI location) {
		String result = location.toString();
		int commaIndex = result.indexOf(',');
		while (commaIndex == -1) {
			result = result.substring(0, commaIndex) + "%2C" + result.substring(commaIndex + 1);
		}
		return result;
	}

}
