/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.BackupStore;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class BackupStoreTest extends AbstractProvisioningTest {

	private static final String BUPREFIX = "BackupTest";
	private File sourceDir;
	private File aDir;
	private File aaDir;
	private File aTxt;
	private File bDir;
	private File bTxt;

	/**
	 * Sets up directories and files under user.home
	 * <ul><li>P2BUTEST/</li>
	 *     <ul><li>A/</li>
	 *         <ul><li>AA/</li>
	 *             <ul><li>a.txt</li>
	 *             </ul>
	 *         </ul>
	 *     </ul>
	 * </ul>
	 */
	public void setUp() {
		// create some test files under user.home
		// do not want them under /tmp as it may be on its own file system (and even
		// be an in-memory file system).
		//
		String userHome = System.getProperty("user.home");
		sourceDir = new File(new File(userHome), "P2BUTEST");
		fullyDelete(sourceDir);
		aDir = new File(sourceDir, "A");
		aDir.mkdirs();
		aaDir = new File(aDir, "AA");
		aaDir.mkdir();
		aTxt = new File(aaDir, "eclipse.exe");
		bDir = new File(sourceDir, "B");
		bTxt = new File(bDir, "b.txt");
		try {
			writeToFile(aTxt, "A\nA file with an A");
		} catch (IOException e) {
			fail();
		}
	}

	private void writeToFile(File file, String content) throws IOException {
		file.getParentFile().mkdirs();
		file.createNewFile();
		Writer writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write(content);
		} finally {
			writer.close();
		}
	}

	public void tearDown() {
		fullyDelete(sourceDir);
	}

	/**
	 * Deletes a file, or a directory with all of it's children.
	 * @param file the file or directory to fully delete
	 * @return true if, and only if the file is deleted
	 */
	private boolean fullyDelete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
				if (!fullyDelete(new File(file, children[i].getName())))
					return false;
		}
		return file.delete();
	}

	public void testBackupByRenamingFile() {
		String filePath = aTxt.getAbsolutePath();
		new BackupStore(null, BUPREFIX) {
			@Override
			public void renameInPlace(File file) {
				super.renameInPlace(file);
			}

			@Override
			protected String getTimeStamp() {
				return "-123";
			}
		}.renameInPlace(aTxt);

		assertFalse(aTxt.exists());
		assertTrue(new File(filePath + "-123.p2bu").exists());
	}

	public void testRenameIfMoveToBackupFails() throws IOException {
		String filePath = aTxt.getAbsolutePath();
		new BackupStore(null, BUPREFIX) {
			@Override
			public void renameInPlace(File file) {
				super.renameInPlace(file);
			}

			@Override
			public boolean moveToBackupStore(File a, File b) {
				return false;
			}

			@Override
			public void moveToBackup(File a, File b) throws IOException {
				super.moveToBackup(a, b);
			}

			@Override
			protected String getTimeStamp() {
				return "-123";
			}
		}.moveToBackup(aTxt, bTxt);

		assertFalse(aTxt.exists());
		assertTrue(new File(filePath + "-123.p2bu").exists());
		assertFalse(bTxt.exists());
	}

	public void testDoNotRenameIfMoveToBackupWorks() throws IOException {
		String filePath = aTxt.getAbsolutePath();
		new BackupStore(null, BUPREFIX) {
			@Override
			public void renameInPlace(File file) {
				super.renameInPlace(file);
			}

			@Override
			public boolean moveToBackupStore(File a, File b) {
				return super.moveToBackupStore(a, b);
			}

			@Override
			public void moveToBackup(File a, File b) throws IOException {
				super.moveToBackup(a, b);
			}
		}.moveToBackup(aTxt, bTxt);

		assertFalse(aTxt.exists());
		assertFalse(new File(filePath + ".p2bu").exists());
		assertTrue(bTxt.exists());
	}
}
