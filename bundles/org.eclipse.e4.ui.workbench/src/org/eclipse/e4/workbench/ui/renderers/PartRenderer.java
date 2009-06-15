/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.workbench.ui.renderers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.e4.core.services.IContributionFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.ui.model.application.ApplicationPackage;
import org.eclipse.e4.ui.model.application.MHandler;
import org.eclipse.e4.ui.model.application.MPart;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;

public class PartRenderer {
	private final List partFactories = new ArrayList();

	// SWT property ids containing the currently rendered part (and factory) for
	// a given widget
	public static final String FACTORY = "partFactory"; //$NON-NLS-1$

	// Life Cycle listeners
	private AdapterImpl visibilityListener = new AdapterImpl() {
		@Override
		public void notifyChanged(Notification msg) {
			if (ApplicationPackage.Literals.MPART__VISIBLE.equals(msg
					.getFeature())) {
				MPart<?> changedPart = (MPart<?>) msg.getNotifier();

				// If the parent isn't displayed who cares?
				MPart<?> parent = changedPart.getParent();
				PartFactory parentFactory = getFactoryFor(parent);
				if (parentFactory == null)
					return;

				if (changedPart.isVisible()) {
					System.out.println("visible -> true"); //$NON-NLS-1$

					// Note that the 'createGui' protocol calls 'childAdded'
					createGui(changedPart);
				} else {
					System.out.println("visible -> false"); //$NON-NLS-1$

					// Note that the 'createGui' protocol calls 'childRemoved'
					removeGui(changedPart);
				}
			}
		}
	};

	private AdapterImpl childrenListener = new AdapterImpl() {
		@Override
		public void notifyChanged(Notification msg) {
			if (ApplicationPackage.Literals.MPART__CHILDREN.equals(msg
					.getFeature())) {
				MPart<?> changedPart = (MPart<?>) msg.getNotifier();
				PartFactory factory = getFactoryFor(changedPart);

				// If the parent isn't in the UI then who cares?
				if (factory == null)
					return;

				if (msg.getEventType() == Notification.ADD) {
					System.out.println("Child Added"); //$NON-NLS-1$
					MPart added = (MPart) msg.getNewValue();
					createGui(added);
				} else if (msg.getEventType() == Notification.REMOVE) {
					System.out.println("Child Removed"); //$NON-NLS-1$
					MPart removed = (MPart) msg.getOldValue();
					if (removed.isVisible())
						removeGui(removed);
				}
			}
		}
	};

	private final IContributionFactory contributionFactory;
	private final IEclipseContext context;

	public PartRenderer(IContributionFactory contributionFactory,
			IEclipseContext context) {
		this.contributionFactory = contributionFactory;
		this.context = context;
	}

	public void addPartFactory(PartFactory factory) {
		partFactories.add(factory);
	}

	public Object createGui(MPart element) {
		// Life-cycle hooks
		installLifeCycleHooks(element);

		if (!element.isVisible())
			return null;

		// Create a control appropriate to the part
		Object newWidget = createWidget(element);

		// Remember that we've created the control
		if (newWidget != null) {
			// Bind the widget to its model element
			element.setWidget(newWidget);

			// Process its internal structure through the factory that created
			// it
			PartFactory factory = getFactoryFor(element);
			factory.bindWidget(element, newWidget);
			hookControllerLogic(element);
			factory.processContents(element);
			factory.postProcess(element);

			// Now that we have a widget let the parent know
			if (element.getParent() instanceof MPart) {
				MPart parentElement = (MPart) element.getParent();
				PartFactory parentFactory = getFactoryFor(parentElement);
				parentFactory.childAdded(parentElement, element);
			}
		}

		return newWidget;
	}

	public void removeGui(MPart element) {
		PartFactory factory = getFactoryFor(element);
		assert (factory != null);

		MPart parent = element.getParent();
		PartFactory parentFactory = getFactoryFor(parent);
		if (parentFactory != null) {
			parentFactory.childRemoved(element.getParent(), element);
		}
		factory.disposeWidget(element);
	}

	/**
	 * @param element
	 *            an element that's been seen by createGui
	 */
	private void installLifeCycleHooks(MPart element) {
		// Handle visibility changes
		if (!((EObject) element).eAdapters().contains(visibilityListener))
			((EObject) element).eAdapters().add(visibilityListener);

		// Handle children
		if (!((EObject) element).eAdapters().contains(childrenListener))
			((EObject) element).eAdapters().add(childrenListener);
	}

	protected Object createWidget(MPart element) {
		// Iterate through the factories until one actually creates the widget
		for (Iterator iterator = partFactories.iterator(); iterator.hasNext();) {
			PartFactory factory = (PartFactory) iterator.next();

			// *** Put any declarative tests here to prevent aggressive loading
			// For example, test whether this factory handles a particular model
			// type ('StackModel'...)

			Object newWidget = factory.createWidget(element);
			if (newWidget != null) {
				// Remember which factory created the widget
				setFactoryFor(element, factory);

				processHandlers(element);
				if (element.getMenu() != null) {
					factory.createMenu(element, newWidget, element.getMenu());
				}
				if (element.getToolBar() != null) {
					factory.createToolBar(element, newWidget, element
							.getToolBar());
				}
				return newWidget;
			}
		}

		return null;
	}

	protected void processHandlers(MPart<?> element) {
		for (MHandler contributedHandler : element.getHandlers()) {
			if (contributedHandler.getURI() != null
					&& contributedHandler.getObject() == null) {
				contributedHandler.setObject(contributionFactory.create(
						contributedHandler.getURI(), context));
			}
		}
	}

	/**
	 * Manages the relationship between a MPart<?> and its rendered Widget.
	 * 
	 * MPart<?>.getWidget().getData(OWNING_ME) == MPart<?>
	 * 
	 * @param element
	 *            The UI element
	 * @param widget
	 *            The widget
	 */
	// public void bindWidget(MPart<?> me, Widget widget) {
	// me.setWidget(widget);
	// widget.setData(OWNING_ME, me);
	//		
	// hookControllerLogic(me, widget);
	// }

	private void hookControllerLogic(final MPart<?> element) {
		// Delegate widget specific hook-up to the creating factory
		PartFactory factory = (PartFactory) getFactoryFor(element);
		if (factory != null)
			factory.hookControllerLogic(element);
	}

	protected void setFactoryFor(MPart element, PartFactory factory) {
		element.setOwner(factory);
	}

	protected PartFactory getFactoryFor(MPart element) {
		return (PartFactory) element.getOwner();
	}
}