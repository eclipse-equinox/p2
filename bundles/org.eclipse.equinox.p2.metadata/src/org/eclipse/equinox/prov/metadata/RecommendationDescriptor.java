/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.metadata;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.prov.metadata.MetadataActivator;
import org.eclipse.osgi.service.resolver.VersionRange;

public class RecommendationDescriptor {
	public static final String TOUCHPOINT_DATA_KEY = "recommendations";
	public static final String KIND = "recommendation";

	private Set recommendations;

	public RecommendationDescriptor(Set recommendations) {
		this.recommendations = recommendations;
	}

	public Set getRecommendations() {
		return recommendations;
	}

	public Recommendation findRecommendation(RequiredCapability toMatch) {
		for (Iterator iterator = recommendations.iterator(); iterator.hasNext();) {
			Recommendation name = (Recommendation) iterator.next();
			if (name.matches(toMatch))
				return name;
		}
		return null;
	}

	public Recommendation findRecommendation(Recommendation toMatch) {
		for (Iterator iterator = recommendations.iterator(); iterator.hasNext();) {
			Recommendation name = (Recommendation) iterator.next();
			if (name.matches(toMatch))
				return name;
		}
		return null;
	}

	//Merge the other descriptor into this one. Return an OK Status is the merged succeeded, otherwise return an INFO Status
	public IStatus merge(RecommendationDescriptor other) {
		MultiStatus result = null;
		for (Iterator it = other.recommendations.iterator(); it.hasNext();) {
			Recommendation otherRecommendation = (Recommendation) it.next();
			Recommendation matchInThis = findRecommendation(otherRecommendation);
			if (matchInThis == null) {
				recommendations.add(otherRecommendation);
				continue;
			}
			Recommendation newRec = otherRecommendation.merge(matchInThis);
			if (newRec != null) {
				recommendations.remove(matchInThis);
				recommendations.add(newRec);
				continue;
			} else {
				if (result == null)
					result = new MultiStatus(MetadataActivator.PI_METADATA, 0, "Conflict between recommendations", null);
				result.add(new Status(IStatus.INFO, MetadataActivator.PI_METADATA, "can't merge " + otherRecommendation + " with " + matchInThis));
			}
		}
		if (result == null)
			return Status.OK_STATUS;
		return result;

	}

	public static RecommendationDescriptor parse(String descriptor) {
		StringTokenizer entries = new StringTokenizer(descriptor, "\n");
		Set recommendations = new HashSet(entries.countTokens());
		while (entries.hasMoreElements()) {
			StringTokenizer oneRec = new StringTokenizer((String) entries.nextElement(), "/");
			if (oneRec.countTokens() != 4) {
				//format error, ignore and continue

				continue;
			}
			String ns = oneRec.nextToken().trim();
			String name = oneRec.nextToken().trim();
			String oldRange = oneRec.nextToken().trim();
			String newRange = oneRec.nextToken().trim();
			recommendations.add(new Recommendation(new RequiredCapability(ns, name, new VersionRange(oldRange)), new RequiredCapability(ns, name, new VersionRange(newRange))));
		}
		return new RecommendationDescriptor(recommendations);
	}

	public static String serialize(RecommendationDescriptor toSerialize) {
		StringBuffer result = new StringBuffer();
		for (Iterator iterator = toSerialize.recommendations.iterator(); iterator.hasNext();) {
			Recommendation entry = (Recommendation) iterator.next();
			result.append(entry.applyOn().getNamespace() + '/' + entry.applyOn().getName() + '/' + entry.applyOn().getRange().toString() + '/' + entry.newValue().getRange().toString() + '\n');
		}
		return result.toString();
	}

	public boolean isCompatible(RecommendationDescriptor other) {
		for (Iterator it = other.recommendations.iterator(); it.hasNext();) {
			Recommendation otherRecommendation = (Recommendation) it.next();
			Recommendation matchInThis = findRecommendation(otherRecommendation);
			if (matchInThis == null) {
				continue;
			}
			if (!otherRecommendation.isCompatible(matchInThis))
				return false;
		}
		return true;

	}
}
