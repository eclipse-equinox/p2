/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.eclipse.equinox.internal.p2.touchpoint.natives.SimpleBackupStore;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class BackupTest extends AbstractProvisioningTest {
	private static final String BUPREFIX = "backup-test";

	private Path sourceDir;

	private Path aDir;
	private Path aaDir;
	private Path aTxt;

	private Path bDir;
	private Path bTxt;

	private Path abDir;

	private Path cTxt;

	private SimpleBackupStore store;

	/**
	 * <pre>
	 * /p2-backup-test
	 *   /a
	 *     /aa
	 *     	 /a.txt
	 *     	 /b.txt
	 *     /ab
	 *     	 /c.txt
	 *   /b
	 * </pre>
	 */
	@Override
	public void setUp() throws IOException {
		// Don't want to backup under /tmp since it may be it's own file system or an
		// in-memory file system
		String userHome = System.getProperty("user.home");

		sourceDir = Path.of(userHome, "p2-backup-test");
		deleteAll(sourceDir);

		aDir = sourceDir.resolve("a");
		Files.createDirectories(aDir);

		aaDir = aDir.resolve("aa");
		Files.createDirectories(aaDir);

		aTxt = aaDir.resolve("a.txt");
		Files.write(aTxt, "A\nA file with an A".getBytes());

		bTxt = aaDir.resolve("b.txt");
		Files.write(bTxt, "B\nA file with a B".getBytes());

		abDir = aDir.resolve("ab");
		Files.createDirectories(abDir);

		cTxt = abDir.resolve("c.txt");
		Files.write(cTxt, "C\nA file with a C".getBytes());

		bDir = sourceDir.resolve("b");
		Files.createDirectories(bDir);

		store = new SimpleBackupStore(null, BUPREFIX);
	}

	@Override
	public void tearDown() throws IOException {
		deleteAll(sourceDir);
	}

	private static void deleteAll(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) {
					throw exc;
				}
				Files.delete(dir);
				return CONTINUE;
			}
		});
	}

	/**
	 * Test that a path containing ".." can be backed up and restored.
	 */
	public void testBackupRelative() throws IOException {
		Path cTxtRelative = aaDir.resolve(aaDir.relativize(cTxt));

		store.backup(cTxtRelative.toFile());
		assertFalse(Files.exists(cTxt));

		Files.write(cTxt, "XXXX\n- This file should be restored with C".getBytes());

		store.restore();
		assertFileContent("Restore of C failed - not original content", cTxt.toFile(), "C");
		assertNoGarbage(store);
	}

	public void testBackupRestore() throws IOException {
		store.backup(aTxt.toFile());
		assertFalse("File not moved to backup - still exists", Files.exists(aTxt));

		Files.write(aTxt, "XXXX\n- This file should be restored with A".getBytes());

		store.backup(bDir.toFile());
		assertFalse("Backed up directory was not moved", Files.isDirectory(bDir));

		store.backupCopy(bTxt.toFile());
		assertFileContent("File should have been copied", bTxt.toFile(), "B");

		store.restore();
		assertFileContent("Restore of A failed - not original content", aTxt.toFile(), "A");
		assertTrue("Empty directory not restored ok", Files.isDirectory(bDir) && Files.list(bDir).count() == 0);
		assertNoGarbage(store);
	}

	public void testBackupDiscard() throws IOException {
		store.backup(aTxt.toFile());
		assertFalse("File not moved to backup - still exists", Files.exists(aTxt));

		Files.write(aTxt, "XXXX\n- This file should be restored with A".getBytes());

		store.backup(bDir.toFile());
		assertFalse("Backed up directory was not moved", Files.exists(bDir));

		store.discard();
		assertFileContent("Discard of a.txt failed - not new content", aTxt.toFile(), "XXXX");
		assertFalse("Empty directory not discarded - still exists", Files.isDirectory(bDir));
		assertNoGarbage(store);
	}

	public void testBackupAll() throws IOException {
		store.backupAll(aDir.toFile());
		assertFalse("File not moved to backup - still exists", Files.exists(aTxt));
		assertFalse("File bTxt not moved to backup - still exists", Files.exists(bTxt));

		Files.createDirectories(aTxt.getParent());
		Files.write(aTxt, "XXXX\n- This file should be restored with A".getBytes(), CREATE_NEW);

		store.restore();
		assertFileContent("A not restored", aTxt.toFile(), "A");
		assertFileContent("B not restored", bTxt.toFile(), "B");
		assertNoGarbage(store);
	}

	public void testBackupCopyAll() throws IOException {
		store.backupCopyAll(aDir.toFile());
		assertTrue("File not copied to backup - does not exist", Files.exists(aTxt));
		assertTrue("File bTxt not copied to backup - does not exists", Files.exists(bTxt));

		Files.write(aTxt, "XXXX\n- This file should be restored with A".getBytes());
		Files.write(bTxt, "XXXX\n- This file should be restored with B".getBytes());

		store.restore();
		assertFileContent("A not restored", aTxt.toFile(), "A");
		assertFileContent("B not restored", bTxt.toFile(), "B");
		assertNoGarbage(store);
	}

	private static void assertNoGarbage(SimpleBackupStore store) {
		File buDir = store.getBackupRoot();
		assertFalse("Backup directory not cleaned up", buDir.exists());
	}
}
