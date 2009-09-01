/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.io.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.BackupStore;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class BackupTest extends AbstractProvisioningTest {
	private static final String BUPREFIX = "BackupTest";
	private File sourceDir;
	private File aDir;
	private File aaDir;
	private File bDir;
	private File aTxt;
	private File bTxt;
	private File abDir;
	private File cTxt;
	private File cTxtRelative;

	/**
	 * Sets up directories and files under user.home
	 * <ul><li>P2BUTEST/</li>
	 *     <ul><li>A/</li>
	 *         <ul><li>AA/</li>
	 *             <ul><li>a.txt</li>
	 *                 <li>b.txt</li>
	 *             </ul>
	 *         </ul>
	 *         <li>B/</li>
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
		aDir = new File(sourceDir, "A");
		aDir.mkdirs();
		aaDir = new File(aDir, "AA");
		aaDir.mkdir();
		abDir = new File(aDir, "AB");
		abDir.mkdir();

		bDir = new File(sourceDir, "B");
		bDir.mkdirs();
		aTxt = new File(aaDir, "a.txt");
		bTxt = new File(aaDir, "b.txt");
		cTxt = new File(abDir, "c.txt");
		cTxtRelative = new File(aaDir, "../AB/c.txt");
		try {
			writeToFile(aTxt, "A\nA file with an A");
			writeToFile(bTxt, "B\nA file with a B");
			writeToFile(cTxt, "C\nA file with a C");
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

	/**
	 * Test that a path containing ".." can be backed up and restored.
	 */
	public void testBackupRelative() {
		BackupStore store = new BackupStore(null, BUPREFIX);
		// backup and overwrite a.txt
		try {
			store.backup(cTxtRelative);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up cTxtRelative");
		}
		if (cTxt.exists())
			fail("File not moved to backup - still exists");
		try {
			writeToFile(cTxt, "XXXX\n- This file should be restored with C");
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not write a file for testing purposes.");
		}

		// restore
		try {
			store.restore();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Restore operation failed with IOException");
		}
		// assert restore
		assertFileContent("Restore of C failed - not original content", cTxt, "C");
		assertNoGarbage(store);
	}

	public void testBackupRestore() {
		BackupStore store = new BackupStore(null, BUPREFIX);
		// backup and overwrite a.txt
		try {
			store.backup(aTxt);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up aTxt");
		}
		if (aTxt.exists())
			fail("File not moved to backup - still exists");
		try {
			writeToFile(aTxt, "XXXX\n- This file should be restored with A");
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not write a file for testing purposes.");
		}

		// backup the empty B directory
		try {
			store.backup(bDir);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up bDir");
		}
		if (bDir.exists())
			fail("Backed up directory was not moved");

		// backup b as a copy
		try {
			store.backupCopy(bTxt);
			assertFileContent("File should have been copied", bTxt, "B");
		} catch (IOException e) {
			fail("Could not backupCopy bTxt");
		}

		// restore
		try {
			store.restore();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Restore operation failed with IOException");
		}

		// assert restore
		assertFileContent("Restore of A failed - not original content", aTxt, "A");
		if (!bDir.isDirectory() && bDir.listFiles().length != 0)
			fail("Empty directory not restored ok");

		assertNoGarbage(store);
	}

	public void testBackupDiscard() {
		BackupStore store = new BackupStore(null, BUPREFIX);
		// backup and overwrite a.txt
		try {
			store.backup(aTxt);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up aTxt");
		}
		if (aTxt.exists())
			fail("File not moved to backup - still exists");
		try {
			writeToFile(aTxt, "XXXX\n- This file should be restored with A");
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not write a file for testing purposes.");
		}

		// backup the empty B directory
		try {
			store.backup(bDir);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up bDir");
		}
		if (bDir.exists())
			fail("Backed up directory was not moved");

		// restore
		store.discard();

		// assert discard
		assertFileContent("Discard of A failed - not new content", aTxt, "XXXX");
		if (bDir.isDirectory())
			fail("Remove of empty directory not discarded ok");

		assertNoGarbage(store);
	}

	public void testBackupAll() {
		BackupStore store = new BackupStore(null, BUPREFIX);
		// backup and overwrite a.txt
		try {
			store.backupAll(aDir);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up aDir");
		}
		if (aTxt.exists())
			fail("File not moved to backup - still exists");
		if (bTxt.exists())
			fail("File bTxt not moved to backup - still exists");

		try {
			writeToFile(aTxt, "XXXX\n- This file should be restored with A");
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not write a file for testing purposes.");
		}
		try {
			store.restore();
		} catch (IOException e) {
			fail("Restore failed");
		}
		assertFileContent("A not restored", aTxt, "A");
		assertFileContent("B not restored", bTxt, "B");
		assertNoGarbage(store);
	}

	public void testBackupCopyAll() {
		BackupStore store = new BackupStore(null, BUPREFIX);
		// backup and overwrite a.txt
		try {
			store.backupCopyAll(aDir);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception when backing up aDir");
		}
		if (!aTxt.exists())
			fail("File not copied to backup - does not exist");
		if (!bTxt.exists())
			fail("File bTxt not copied to backup - does not exists");

		try {
			writeToFile(aTxt, "XXXX\n- This file should be restored with A");
			writeToFile(bTxt, "XXXX\n- This file should be restored with B");
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not write a file for testing purposes.");
		}
		try {
			store.restore();
		} catch (IOException e) {
			fail("Restore failed");
		}
		assertFileContent("A not restored", aTxt, "A");
		assertFileContent("B not restored", bTxt, "B");
		assertNoGarbage(store);
	}

	private void assertNoGarbage(BackupStore store) {
		File buDir = new File(store.getBackupRoot(), BUPREFIX);
		if (buDir.exists())
			fail("Backup directory not cleaned up");

		//		Set roots = store.getBackupRoots();
		//		if (roots.size() == 0)
		//			assertTrue("Root set is empty", true);
		//		for (Iterator itor = roots.iterator(); itor.hasNext();) {
		//			File root = (File) itor.next();
		//			File buDir = new File(root, BUPREFIX);
		//			if (buDir.exists())
		//				fail("Backup directory not cleaned up");
		//		}
	}
}
