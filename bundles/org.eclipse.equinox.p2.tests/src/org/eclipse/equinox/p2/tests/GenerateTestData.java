/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.util.*;
import org.osgi.framework.Version;

/*
 * Class which can generate features and bundles for test data.
 * See #printUsage for arguments.
 */
public class GenerateTestData {

	static final String baseBundleName = "My Bundle";
	static final String baseBundleId = "com.example.bundle";
	static final Version bundleVersion = new Version("1.0");

	static final String baseFeatureName = "My feature";
	static final String baseFeatureId = "com.example.feature";
	static final Version featureVersion = new Version("1.0");

	static int numFeatures = 0;
	static int numBundles = 0;
	static int numRequires = 0;
	static File outputFile = null;
	static Random random = new Random(System.currentTimeMillis());

	Set<TestFeature> features = new HashSet<TestFeature>();
	List<TestBundle> bundles = new ArrayList<TestBundle>();

	/*
	 * Abstract class representing a feature or a bundle.
	 */
	static abstract class TestObject {
		String name;
		String id;
		Version version;
		// use a set to avoid duplicates
		Set<TestObject> requires = new HashSet<TestObject>();

		public boolean equals(Object obj) {
			if (!(obj instanceof TestBundle))
				return false;
			TestBundle other = (TestBundle) obj;
			return id.equals(other.id) && version.equals(other.version);
		}

		public int hashCode() {
			return id.hashCode() + version.hashCode();
		}

		// subclasses override to write out feature.xml or manifest.mf
		abstract String getManifest();
	}

	static class TestBundle extends TestObject {

		String getManifest() {
			StringBuffer manifest = new StringBuffer();
			manifest.append("Manifest-Version: 1.0\n");
			manifest.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
			manifest.append("Bundle-ManifestVersion: 2\n");
			manifest.append("Bundle-Name: " + name + '\n');
			manifest.append("Bundle-SymbolicName: " + id + '\n');
			manifest.append("Bundle-Version: " + version + '\n');
			if (!requires.isEmpty()) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("Require-Bundle: ");
				for (Iterator<TestObject> iter = requires.iterator(); iter.hasNext();) {
					TestObject req = iter.next();
					buffer.append(req.id);
					if (iter.hasNext())
						buffer.append(",\n ");
				}
				manifest.append(buffer.toString());
			}
			return manifest.toString();
		}

	}

	static class TestFeature extends TestObject {

		String getManifest() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			buffer.append("	<feature\n");
			buffer.append("\t\tid=\"" + id + "\"\n");
			buffer.append("\t\tlabel=\"" + name + "\"\n");
			buffer.append("\t\tversion=\"" + version + "\"\n");
			buffer.append("\t\tprovider-name=\"providerName\"\n");
			buffer.append("\t\timage=\"eclipse_update_120.jpg\">\n");
			buffer.append("\t<description>\n");
			buffer.append("\t\tdescription\n");
			buffer.append("\t</description>\n");
			buffer.append("\t<copyright>\n");
			buffer.append("\t\tcopyright\n");
			buffer.append("\t</copyright>\n");
			buffer.append("\t<license url=\"licenseURL\">\n");
			buffer.append("\t\tlicense\n");
			buffer.append("\t</license>\n");
			for (TestObject bundle : requires) {
				buffer.append("\t<plugin\n");
				buffer.append("\t\tid=\"" + bundle.id + "\"\n");
				buffer.append("\t\tdownload-size=\"0\"\n");
				buffer.append("\t\tinstall-size=\"0\"\n");
				buffer.append("\t\tversion=\"" + bundle.version + "\"/>\n");
			}
			buffer.append("</feature>\n");
			return buffer.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		processCommandLineArgs(args);
		if (!validate()) {
			printUsage();
			return;
		}
		new GenerateTestData().generate();
	}

	static void printUsage() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("\nUse this application to generate test feature and bundle data.\n");
		buffer.append("Parameters:\n");
		buffer.append("-output <location>\n");
		buffer.append("-features <number of features>\n");
		buffer.append("-bundles <number of bundles>\n");
		buffer.append("-requires <max number of requires for each bundle> [optional]\n");
		buffer.append("\tSpecifying a requires of X bundles picks up to X of previously created bundles to be required.\n");
		buffer.append("\tThis means bundles produced at the end are more likely to have X requirements.\n");
		buffer.append("\tThis is done to avoid circular dependencies.\n");
		System.err.println(buffer.toString());
	}

	/*
	 * Verify setup before starting. Ensure all required parameters have been set.
	 */
	static boolean validate() {
		StringBuffer buffer = new StringBuffer();
		if (outputFile == null)
			buffer.append("Need to set an output directory.\n");
		if (numFeatures < 0 || numBundles <= 0)
			buffer.append("Need to specifiy at least one bundle.\n");
		if (numFeatures > numBundles)
			buffer.append("Number of features must be the same or less than the number of bundles.\n");
		if (numRequires > numBundles - 1)
			buffer.append("Cannot have more required bundles than bundles.\n");
		if (buffer.length() == 0)
			return true;
		System.err.println(buffer.toString());
		return false;
	}

	static void processCommandLineArgs(String[] args) {
		for (int i = 0; i < args.length - 1; i++) {
			String arg = args[i];
			String next = args[++i];
			if ("-features".equalsIgnoreCase(arg)) {
				numFeatures = Integer.parseInt(next);
			} else if ("-bundles".equalsIgnoreCase(arg) || "-plugins".equalsIgnoreCase(arg)) {
				numBundles = Integer.parseInt(next);
			} else if ("-requires".equalsIgnoreCase(arg)) {
				numRequires = Integer.parseInt(next);
			} else if ("-output".equalsIgnoreCase(arg)) {
				outputFile = new File(next);
			}
		}
	}

	void generate() throws IOException {
		System.out.println("Generating " + numBundles + " bundles.");
		for (int i = 0; i < numBundles; i++) {
			TestBundle bundle = generateBundle(baseBundleName + i, baseBundleId + i, bundleVersion);
			bundles.add(bundle);
		}

		System.out.println("Generating " + numFeatures + " features.");
		for (int i = 0; i < numFeatures; i++) {
			TestFeature feature = generateFeature(baseFeatureName + i, baseFeatureId + i, featureVersion, i);
			features.add(feature);
		}

		save();
	}

	/*
	 * Persist the features and bundles to disk.
	 */
	void save() throws IOException {
		outputFile.mkdirs();
		File featuresDir = new File(outputFile, "features");
		featuresDir.mkdirs();
		File pluginsDir = new File(outputFile, "plugins");
		pluginsDir.mkdirs();

		System.out.println("Writing " + features.size() + " features.");
		for (TestFeature feature : features) {
			File featureDir = new File(featuresDir, feature.id + '_' + feature.version);
			File location = new File(featureDir, "feature.xml");
			write(location, feature.getManifest());
		}

		System.out.println("Writing " + bundles.size() + " bundles.");
		for (TestBundle bundle : bundles) {
			File pluginDir = new File(pluginsDir, bundle.id + '_' + bundle.version);
			File location = new File(new File(pluginDir, "META-INF"), "MANIFEST.MF");
			write(location, bundle.getManifest());
		}
	}

	void write(File location, String data) throws IOException {
		File parent = location.getParentFile();
		if (parent == null)
			throw new RuntimeException("Unable to write to: " + location.getAbsolutePath() + "due to null parent.");
		parent.mkdirs();
		OutputStream output = null;
		try {
			output = new BufferedOutputStream(new FileOutputStream(location));
			output.write(data.getBytes());
		} finally {
			if (output != null)
				try {
					output.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	// Return a random bundle from the collection of already produced ones.
	TestBundle getRandomBundle() {
		if (bundles.isEmpty())
			return null;
		int index = random.nextInt(bundles.size());
		return bundles.get(index);
	}

	TestBundle generateBundle(String name, String id, Version version) {
		TestBundle bundle = new TestBundle();
		bundle.id = id;
		bundle.name = name;
		bundle.version = version;
		for (int i = 0; i < numRequires; i++) {
			TestBundle req = getRandomBundle();
			if (req != null)
				bundle.requires.add(req);
		}
		return bundle;
	}

	TestFeature generateFeature(String name, String id, Version version, int index) {
		TestFeature feature = new TestFeature();
		feature.id = id;
		feature.name = name;
		feature.version = version;
		// bundles were generated first and we already checked that we have more/equal bundles and features
		// so we should be ok here.
		TestBundle bundle = bundles.get(index);
		if (bundle == null)
			throw new RuntimeException("Could not find bundle at index: " + index + " to match feature: " + id);
		feature.requires.add(bundle);
		return feature;
	}

}
