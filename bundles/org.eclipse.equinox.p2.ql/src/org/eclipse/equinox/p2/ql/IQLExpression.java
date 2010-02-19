package org.eclipse.equinox.p2.ql;

import org.eclipse.equinox.p2.metadata.expression.IExpression;

public interface IQLExpression extends IExpression {
	int TYPE_ARRAY = 20;
	int TYPE_ASSIGNMENT = 21;
	int TYPE_COLLECT = 22;
	int TYPE_CONDITION = 23;
	int TYPE_FIRST = 24;
	int TYPE_FLATTEN = 25;
	int TYPE_FUNCTION = 26;
	int TYPE_INTERSECT = 27;
	int TYPE_LATEST = 28;
	int TYPE_LIMIT = 29;
	int TYPE_PIPE = 30;
	int TYPE_SELECT = 31;
	int TYPE_TRAVERSE = 32;
	int TYPE_UNION = 33;
	int TYPE_UNIQUE = 34;
}
