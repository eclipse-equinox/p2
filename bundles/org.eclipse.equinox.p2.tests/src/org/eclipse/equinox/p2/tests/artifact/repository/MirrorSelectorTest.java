/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     martin.kirst@s1998.tu-chemnitz.de - fixed and improved sort algorithm tests
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector.MirrorInfo;

/**
 * 
 */
public class MirrorSelectorTest extends TestCase {

	private List<MirrorInfo> originals;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// examples taken from real live.
		// This is the expected order of mirrors,
		// doesn't matter how often you're resorting ;-)

		originals = new ArrayList<MirrorSelector.MirrorInfo>();
		MirrorInfo mi = null;

		mi = new MirrorInfo("http://ftp.wh2.tu-dresden.de/pub/mirrors/eclipse/", 3);
		mi.setBytesPerSecond(224906);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp-stud.fht-esslingen.de/pub/Mirrors/eclipse/", 1);
		mi.setBytesPerSecond(125868);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://mirror.netcologne.de/eclipse//", 0);
		mi.setBytesPerSecond(199719);
		mi.incrementFailureCount();
		mi.incrementFailureCount();
		//mi.totalFailureCount = 2;
		originals.add(mi);

		mi = new MirrorInfo("http://mirror.selfnet.de/eclipse/", 5);
		mi.setBytesPerSecond(132379);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://mirror.switch.ch/eclipse/", 7);
		mi.setBytesPerSecond(137107);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://www.rcp-vision.com/eclipse/eclipseMirror/", 8);
		mi.setBytesPerSecond(128472);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.mirror.garr.it/mirrors/eclipse//", 10);
		mi.setBytesPerSecond(129359);
		mi.incrementFailureCount();
		mi.incrementFailureCount();
		//mi.totalFailureCount = 2;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.roedu.net/pub/mirrors/eclipse.org/", 6);
		mi.setBytesPerSecond(59587);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://giano.com.dist.unige.it/eclipse/", 9);
		mi.setBytesPerSecond(85624);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.roedu.net/mirrors/eclipse.org//", 19);
		mi.setBytesPerSecond(149572);
		mi.incrementFailureCount();
		mi.incrementFailureCount();
		//mi.totalFailureCount = 2;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.ing.umu.se/mirror/eclipse/", 18);
		mi.setBytesPerSecond(105858);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://mirrors.fe.up.pt/pub/eclipse//", 15);
		mi.setBytesPerSecond(67202);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.heanet.ie/pub/eclipse//", 17);
		mi.setBytesPerSecond(68067);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.sh.cvut.cz/MIRRORS/eclipse/", 21);
		mi.setBytesPerSecond(73659);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.man.poznan.pl/eclipse/", 22);
		mi.setBytesPerSecond(73446);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.dcc.fc.up.pt/", 16);
		mi.setBytesPerSecond(45175);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.nordnet.fi/eclipse/", 23);
		mi.setBytesPerSecond(61443);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://www.gtlib.gatech.edu/pub/eclipse/", 26);
		mi.setBytesPerSecond(57637);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.osuosl.org/pub/eclipse//", 28);
		mi.setBytesPerSecond(35928);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://mirrors.med.harvard.edu/eclipse//", 32);
		mi.setBytesPerSecond(40683);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://mirrors.ibiblio.org/pub/mirrors/eclipse/", 31);
		mi.setBytesPerSecond(34207);
		mi.incrementFailureCount();
		mi.incrementFailureCount();
		//mi.totalFailureCount = 2;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.ussg.iu.edu/eclipse/", 33);
		mi.setBytesPerSecond(31402);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://mirrors.xmission.com/eclipse/", 29);
		mi.setBytesPerSecond(24147);
		mi.incrementFailureCount();
		//mi.totalFailureCount = 1;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.osuosl.org/pub/eclipse/", 34);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://www.ftp.saix.net/Eclipse//", 40);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.daum.net/eclipse/", 41);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.stu.edu.tw/", 43);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.stu.edu.tw/", 44);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.kaist.ac.kr/eclipse/", 45);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.stu.edu.tw//", 46);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.tsukuba.wide.ad.jp/software/eclipse//", 47);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://mirror.neu.edu.cn/eclipse/", 50);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://mirror.bit.edu.cn/eclipse/", 51);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.cs.pu.edu.tw/pub/eclipse/", 52);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://ftp.neu.edu.cn/mirrors/eclipse/", 53);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://download.actuatechina.com/eclipse/", 54);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://linorg.usp.br/eclipse/", 57);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://eclipse.c3sl.ufpr.br/", 59);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

		mi = new MirrorInfo("http://download.eclipse.org/", 61);
		mi.setBytesPerSecond(-1);
		//mi.totalFailureCount = 0;
		originals.add(mi);

	}

	public void testSorting() {

		long maxBytesPerSecond = 0;
		for (MirrorInfo x : originals) {
			maxBytesPerSecond = Math.max(maxBytesPerSecond, x.getBytesPerSecond());
		}

		MirrorSelector.MirrorInfoComparator comparator = new MirrorSelector.MirrorInfoComparator(maxBytesPerSecond, 0, 0);

		// do 1000 tries of randomize and new sort
		// the result should always be the same
		// This way we hopefully get sure, that contract of Comparator#compareTo
		// is fulfilled.
		for (int x = 0; x < 1000; x++) {
			ArrayList<MirrorInfo> templist = new ArrayList<MirrorInfo>(originals);
			Collections.shuffle(templist);
			MirrorInfo[] mirrors = templist.toArray(new MirrorInfo[originals.size()]);
			Arrays.sort(mirrors, comparator);
			assertList(originals, mirrors);
			/*
			 * ================================================================
			 * 
			 * Because of 
			 * Bug 317785 - Synchronization problem in mirror selection
			 * 
			 * We need an implementation of TimSort for this test.
			 * But because of incompatibility of EPL and GPL, the TimSort
			 * algorithm was removed.
			 * Instead, this test relies on the fact, that a proper implementation
			 * of Comparator will always compute the same result.
			 * When this test case runs within a JVM7, it will automatically
			 * use the new TimSort algorithm.
			 * ================================================================
			 */
		}

	}

	public void testComparatorZeros() {

		MirrorSelector.MirrorInfoComparator comparator = new MirrorSelector.MirrorInfoComparator(0, 0, 0);

		assertEquals("equals", comparator.compare(originals.get(0), originals.get(0)), 0);
		assertEquals("equals", comparator.compare(originals.get(1), originals.get(1)), 0);

	}

	/**
	 * @param originallist
	 * @param mirrors
	 */
	private void assertList(List<MirrorInfo> originallist, MirrorInfo[] mirrors) {
		assertEquals("length", originallist.size(), mirrors.length);
		for (int i = 0; i < originallist.size(); i++) {
			assertEquals("equal mirror_" + i, originallist.get(i), mirrors[i]);
		}
	}

}
