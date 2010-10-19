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
package org.eclipse.e4.core.internal.contexts.debug.ui;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.e4.core.contexts.IEclipseContext;

public class AllocationRecorder {

	static private AllocationRecorder defaultRecorder;

	private Map<IEclipseContext, Throwable> traces = Collections.synchronizedMap(new WeakHashMap<IEclipseContext, Throwable>());

	static public AllocationRecorder getDefault() {
		if (defaultRecorder == null)
			defaultRecorder = new AllocationRecorder();
		return defaultRecorder;
	}

	public AllocationRecorder() {
		// placeholder
	}

	public void allocated(IEclipseContext context, Throwable exception) {
		traces.put(context, exception);
	}

	public void disposed(IEclipseContext context) {
		traces.remove(context);
	}

	public Throwable getTrace(IEclipseContext context) {
		return traces.get(context);
	}
}
