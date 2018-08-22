/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.util.ArrayList;
import java.util.List;

public class StringHelper {

	public static String[] EMPTY_ARRAY = new String[0];

	public static String[] getArrayFromString(String spec, char c) {
		String[] resultArr = EMPTY_ARRAY;
		if (spec != null) {
			int splitIdx = spec.indexOf(c);
			if (splitIdx <= 0) {
				spec = spec.trim();
				if (spec.length() > 0)
					resultArr = new String[] {spec};
			} else {
				List<String> result = new ArrayList<>();
				while (splitIdx >= 0) {
					String part = spec.substring(0, splitIdx).trim();
					if (part.length() > 0)
						result.add(part);
					spec = spec.substring(splitIdx + 1);
					splitIdx = spec.indexOf(c);
				}
				spec = spec.trim();
				if (spec.length() > 0)
					result.add(spec);
				resultArr = result.toArray(new String[result.size()]);
			}
		}
		return resultArr;
	}
}
