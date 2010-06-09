/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.menus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.e4.compatibility.E4Util;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.internal.services.RegistryPersistence;

/**
 * <p>
 * A static class for accessing the registry.
 * </p>
 * <p>
 * This class is not intended for use outside of the
 * <code>org.eclipse.ui.workbench</code> plug-in.
 * </p>
 * 
 * @since 3.2
 */
final class MenuPersistence extends RegistryPersistence {

	private MApplication application;
	private IEclipseContext appContext;
	private ArrayList<MenuAdditionCacheEntry> contributions = new ArrayList<MenuAdditionCacheEntry>();

	/**
	 * Constructs a new instance of {@link MenuPersistence}.
	 * 
	 * @param workbenchMenuService
	 * 
	 * @param workbenchMenuService
	 *            The menu service which should be populated with the values
	 *            from the registry; must not be <code>null</code>.
	 */
	MenuPersistence(MApplication application, IEclipseContext appContext) {
		this.application = application;
		this.appContext = appContext;
	}

	public final void dispose() {
		for (MenuAdditionCacheEntry mc : contributions) {
			mc.dispose();
		}
		contributions.clear();
		super.dispose();
	}

	protected final boolean isChangeImportant(final IRegistryChangeEvent event) {
		/*
		 * TODO Menus will need to be re-read (i.e., re-verified) if any of the
		 * menu extensions change (i.e., menus), or if any of the command
		 * extensions change (i.e., action definitions).
		 */
		return false;
	}

	public boolean menusNeedUpdating(final IRegistryChangeEvent event) {
		final IExtensionDelta[] menuDeltas = event.getExtensionDeltas(
				PlatformUI.PLUGIN_ID, IWorkbenchRegistryConstants.PL_MENUS);
		if (menuDeltas.length == 0) {
			return false;
		}

		return true;
	}

	/**
	 * <p>
	 * Reads all of the menu elements and action sets from the registry.
	 * </p>
	 * <p>
	 * TODO Add support for modifications.
	 * </p>
	 */
	protected final void read() {
		super.read();

		// Read legacy 3.2 'trim' additions
		readTrimAdditions();

		// read the 3.3 menu additions
		readAdditions();
	}

	//
	// 3.3 menu extension code
	// 

	public void readTrimAdditions() {
		// if (menuService == null)
		// return;
		//
		// final IExtensionRegistry registry = Platform.getExtensionRegistry();
		// final IConfigurationElement[] configElements = registry
		// .getConfigurationElementsFor(EXTENSION_MENUS);
		//
		// // Create a cache entry for every menu addition
		// for (int i = 0; i < configElements.length; i++) {
		// // Only process 'group' entries
		// if (!TAG_GROUP.equals(configElements[i].getName()))
		// continue;
		//
		// String id = configElements[i]
		// .getAttribute(IWorkbenchRegistryConstants.ATT_ID);
		//
		// // Define the initial URI spec
		//			String uriSpec = "toolbar:" + id; //$NON-NLS-1$
		// if (configElements[i].getChildren(TAG_LOCATION).length > 0) {
		// IConfigurationElement location = configElements[i]
		// .getChildren(TAG_LOCATION)[0];
		// if (location.getChildren(TAG_ORDER).length > 0) {
		// IConfigurationElement order = location
		// .getChildren(TAG_ORDER)[0];
		//
		// String pos = order
		// .getAttribute(IWorkbenchRegistryConstants.ATT_POSITION);
		// String relTo = order
		// .getAttribute(IWorkbenchRegistryConstants.ATT_RELATIVE_TO);
		//					uriSpec += "?" + pos + "=" + relTo; //$NON-NLS-1$ //$NON-NLS-2$
		//
		// // HACK! We expect that the new trim group is -always-
		// // relative to
		// // one of the 'default' groups; indicating which trim area
		// // they're in
		// MenuLocationURI uri = new MenuLocationURI(
		//							"toolbar:" + relTo); //$NON-NLS-1$
		// List trimAdditions = menuService.getAdditionsForURI(uri);
		//
		// //
		// // TODO convert the TrimAdditionCacheEntry over to use the
		// // new MenuCacheEntry and addCacheForURI(*)
		// // OK, add the addition to this area
		// uri = new MenuLocationURI(uriSpec);
		// trimAdditions.add(new TrimAdditionCacheEntry(
		// configElements[i], uri, menuService));
		// } else {
		// // Must be a default group; make a new entry cache
		// MenuLocationURI uri = new MenuLocationURI(uriSpec);
		//
		// // NOTE: 'getAdditionsForURI' forces creation
		// menuService.getAdditionsForURI(uri);
		// }
		// }
		// }
	}

	public void readAdditions() {
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		ArrayList configElements = new ArrayList();

		final IConfigurationElement[] menusExtensionPoint = registry
				.getConfigurationElementsFor(EXTENSION_MENUS);

		// Create a cache entry for every menu addition;
		for (int i = 0; i < menusExtensionPoint.length; i++) {
			if (PL_MENU_CONTRIBUTION.equals(menusExtensionPoint[i].getName())) {
				configElements.add(menusExtensionPoint[i]);
			}
		}
		Comparator comparer = new Comparator() {
			public int compare(Object o1, Object o2) {
				IConfigurationElement c1 = (IConfigurationElement) o1;
				IConfigurationElement c2 = (IConfigurationElement) o2;
				return c1.getNamespaceIdentifier().compareToIgnoreCase(
						c2.getNamespaceIdentifier());
			}
		};
		Collections.sort(configElements, comparer);

		Iterator i = configElements.iterator();
		while (i.hasNext()) {
			final IConfigurationElement configElement = (IConfigurationElement) i
					.next();
			
			
			if (isProgramaticContribution(configElement)) {
				// newFactory = new ProxyMenuAdditionCacheEntry(
				// configElement
				// .getAttribute(IWorkbenchRegistryConstants.TAG_LOCATION_URI),
				// configElement.getNamespaceIdentifier(), configElement);\
				E4Util.unsupported("Programmatic Contribution Factories not supported"); //$NON-NLS-1$

			} else {
				MenuAdditionCacheEntry menuContribution = new MenuAdditionCacheEntry(application, appContext,
						configElement,
						configElement
								.getAttribute(IWorkbenchRegistryConstants.TAG_LOCATION_URI),
						configElement.getNamespaceIdentifier());
				contributions.add(menuContribution);
				menuContribution.addToModel();
			}
		}
	}
	
	/**
	 * Return whether or not this contribution is programmatic (ie: has a class attribute).
	 * 
	 * @param menuAddition
	 * @return whether or not this contribution is programamtic
	 * @since 3.5
	 */
	private boolean isProgramaticContribution(IConfigurationElement menuAddition) {
		return menuAddition.getAttribute(IWorkbenchRegistryConstants.ATT_CLASS) != null;
	}
}
