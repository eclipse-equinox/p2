/*******************************************************************************
 * Copyright (c) 2006, 2009 Cloudsmith Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 * 	Cloudsmith Inc - initial API and implementation
 * 	IBM Corporation - ongoing development
 * 	Genuitec - Bug 291926
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.net.URI;
import java.text.NumberFormat;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.osgi.util.NLS;

/**
 * Converts progress of file download to average download speed and keeps track of
 * when it is suitable to update a progress monitor. A suitably scaled and formatted string for use
 * in progress monitoring is provided.
 */
public class ProgressStatistics {
	private static final int DEFAULT_REPORT_INTERVAL = 1000;

	private static final int SPEED_INTERVAL = 5000;

	private static final int SPEED_RESOLUTION = 1000;

	private static String convert(long amount) {
		NumberFormat fmt = NumberFormat.getInstance();
		if (amount < 1024)
			return fmt.format(amount) + "B"; //$NON-NLS-1$

		fmt.setMaximumFractionDigits(2);
		if (amount < 1024 * 1024)
			return fmt.format(((double) amount) / 1024) + "kB"; //$NON-NLS-1$

		return fmt.format(((double) amount) / (1024 * 1024)) + "MB"; //$NON-NLS-1$
	}

	private final String m_fileName;

	private final long m_total;

	private final long m_startTime;

	private long m_current;

	private long m_lastReportTime;

	private int m_reportInterval;

	private SortedMap<Long, Long> m_recentSpeedMap;

	private long m_recentSpeedMapKey;

	private URI m_uri;

	public ProgressStatistics(URI uri, String fileName, long total) {
		m_startTime = System.currentTimeMillis();
		m_fileName = fileName;

		m_total = total;

		m_current = 0;
		m_lastReportTime = 0;
		m_reportInterval = DEFAULT_REPORT_INTERVAL;
		m_recentSpeedMap = new TreeMap<Long, Long>();
		m_recentSpeedMapKey = 0L;
		m_uri = uri;
	}

	public long getAverageSpeed() {
		long dur = getDuration();
		if (dur > 0)
			return (int) (m_current / (dur / 1000.0));
		return 0L;
	}

	public long getDuration() {
		return System.currentTimeMillis() - m_startTime;
	}

	public double getPercentage() {
		if (m_total > 0)
			return ((double) m_current) / ((double) m_total);

		return 0.0;
	}

	synchronized public long getRecentSpeed() {
		removeObsoleteRecentSpeedData(getDuration() / SPEED_RESOLUTION);
		long dur = 0L;
		long amount = 0L;
		SortedMap<Long, Long> relevantData = m_recentSpeedMap.headMap(new Long(m_recentSpeedMapKey));

		if (!relevantData.isEmpty()) {
			for (Long rl : relevantData.values()) {
				dur += SPEED_RESOLUTION;
				amount += rl.longValue();
			}
		}

		if (dur >= 1000)
			return amount / (dur / 1000);

		return 0L;
	}

	public int getReportInterval() {
		return m_reportInterval;
	}

	public long getTotal() {
		return m_total;
	}

	public void increase(long inc) {
		registerRecentSpeed(getDuration() / SPEED_RESOLUTION, inc);
		m_current += inc;
	}

	public synchronized String report() {
		return m_total != -1 ? NLS.bind(Messages.fetching_0_from_1_2_of_3_at_4, new String[] {m_fileName, m_uri.toString(), convert(m_current), convert(m_total), convert(getRecentSpeed())}) : NLS.bind(Messages.fetching_0_from_1_2_at_3, new String[] {m_fileName, m_uri.toString(), convert(m_current), convert(getRecentSpeed())});
	}

	public void setReportInterval(int reportInterval) {
		m_reportInterval = reportInterval;
	}

	public boolean shouldReport() {
		long currentTime = System.currentTimeMillis();
		if (m_lastReportTime == 0 || currentTime - m_lastReportTime >= m_reportInterval) {
			m_lastReportTime = currentTime;
			return true;
		}
		return false;
	}

	public String toString() {
		return report();
	}

	synchronized private void registerRecentSpeed(long key, long inc) {
		Long keyL = new Long(key);
		Long currentValueL = m_recentSpeedMap.get(keyL);
		long currentValue = 0L;
		if (currentValueL != null)
			currentValue = currentValueL.longValue();

		m_recentSpeedMap.put(keyL, new Long(inc + currentValue));

		if (m_recentSpeedMapKey != key) {
			m_recentSpeedMapKey = key;
			removeObsoleteRecentSpeedData(key);
		}
	}

	synchronized private void removeObsoleteRecentSpeedData(long lastKey) {
		long threshold = lastKey - SPEED_INTERVAL / SPEED_RESOLUTION;
		m_recentSpeedMap.headMap(new Long(threshold)).clear();
	}
}
