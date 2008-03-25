package org.eclipse.equinox.internal.p2.publisher.actions;

import java.util.*;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;

public class CopyOfBundleAdvice implements IBundleAdvice {

	Map shapes = new HashMap(11);

	public String getShape(String id, String version) {
		Object[] values = (Object[]) shapes.get(id);
		// if no one says anything then don't say anything.  someone else might have an opinion
		if (values == null)
			return null;

		// otherwise if the object is mentioned, assume its shape is DIR
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(version))
				return IBundleAdvice.DIR;
		}
		// if no one says anything then don't say anything.  someone else might have an opinion
		return null;
	}

	public void setShape(String id, String version, String shape) {
		Object[] values = (Object[]) shapes.get(id);
		if (values == null) {
			values = new Object[] {version};
			shapes.put(id, values);
		} else {
			Object[] newObjects = new Object[values.length + 1];
			System.arraycopy(values, 0, newObjects, 0, values.length);
			newObjects[values.length] = version;
			shapes.put(id, newObjects);
		}
	}

	public IPublishingAdvice merge(IPublishingAdvice advice) {
		if (!(advice instanceof CopyOfBundleAdvice))
			return this;
		CopyOfBundleAdvice source = (CopyOfBundleAdvice) advice;
		for (Iterator i = source.shapes.keySet().iterator(); i.hasNext();) {
			String id = (String) i.next();
			Object[] myValues = (Object[]) shapes.get(id);
			Object[] sourceValues = (Object[]) source.shapes.get(id);
			if (myValues == null)
				shapes.put(id, sourceValues);
			else if (sourceValues != null)
				shapes.put(id, merge(myValues, sourceValues));
		}
		return this;
	}

	private Object[] merge(Object[] myValues, Object[] sourceValues) {
		List result = Arrays.asList(myValues);
		for (int i = 0; i < sourceValues.length; i++) {
			Object object = sourceValues[i];
			boolean found = false;
			for (Iterator j = result.iterator(); j.hasNext();) {
				if (j.next().equals(object)) {
					found = true;
					break;
				}
			}
			if (!found)
				result.add(object);
		}
		return (Object[]) result.toArray(new Object[result.size()]);
	}
}
