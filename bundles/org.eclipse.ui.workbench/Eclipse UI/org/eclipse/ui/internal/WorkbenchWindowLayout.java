/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;



/**
 * The layout for the workbench window's shell.
 */
class WorkbenchWindowLayout extends Layout {

	private final WorkbenchWindow window;

	/**
	 * @param WorkbenchWindow
	 */
	WorkbenchWindowLayout(WorkbenchWindow window) {
		this.window = window;
		// TODO Auto-generated constructor stub
	}

	protected Point computeSize(
		Composite composite,
		int wHint,
		int hHint,
		boolean flushCache) {
		if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
			return new Point(wHint, hHint);

		Point result = new Point(0, 0);
		Control[] ws = composite.getChildren();
		for (int i = 0; i < ws.length; i++) {
			Control w = ws[i];
			boolean skip = false;
			if (w == this.window.getToolBarControl()) {
				skip = true;
				result.y += WorkbenchWindow.BAR_SIZE;
			} else if (
				this.window.getShortcutBar() != null
					&& w == this.window.getShortcutBar().getControl()) {
				skip = true;
			}
			if (!skip) {
				Point e = w.computeSize(wHint, hHint, flushCache);
				result.x = Math.max(result.x, e.x);
				result.y += e.y + WorkbenchWindow.VGAP;
			}
		}

		result.x += WorkbenchWindow.BAR_SIZE; // For shortcut bar.
		if (wHint != SWT.DEFAULT)
			result.x = wHint;
		if (hHint != SWT.DEFAULT)
			result.y = hHint;
		return result;
	}

	protected void layout(Composite composite, boolean flushCache) {
		Rectangle clientArea = composite.getClientArea();

		//Null on carbon
		if (window.getPrimarySeperator()!= null) {
			//Layout top seperator
			Point sep1Size =
				window.getPrimarySeperator().computeSize(
					SWT.DEFAULT,
					SWT.DEFAULT,
					flushCache);
			window.getPrimarySeperator().setBounds(
				clientArea.x,
				clientArea.y,
				clientArea.width,
				sep1Size.y);
			clientArea.y += sep1Size.y;
			clientArea.height -= sep1Size.y;
		}

		int toolBarWidth = clientArea.width;

		//Layout the toolbar	
		Control toolBar = this.window.getToolBarControl();
		if (toolBar != null) {
			if (this.window.getShowToolBar()) {
				int height = WorkbenchWindow.BAR_SIZE;

				if (this.window.toolBarChildrenExist()) {
					Point toolBarSize =
						toolBar.computeSize(
							clientArea.width,
							SWT.DEFAULT,
							flushCache);
					height = toolBarSize.y;
				}
				toolBar.setBounds(
					clientArea.x,
					clientArea.y,
					toolBarWidth,
					height);
				clientArea.y += height;
				clientArea.height -= height;
			} else
				this.window.getToolBarControl().setBounds(0, 0, 0, 0);
		}

		//Layout side seperator
		Control sep2 = this.window.getSeparator2();
		if (sep2 != null) {
			if (this.window.getShowToolBar()) {
				Point sep2Size =
					sep2.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				sep2.setBounds(
					clientArea.x,
					clientArea.y,
					clientArea.width,
					sep2Size.y);
				clientArea.y += sep2Size.y;
				clientArea.height -= sep2Size.y;
			} else
				sep2.setBounds(0, 0, 0, 0);
		}

		int width = WorkbenchWindow.BAR_SIZE;
		//Layout the progress indicator
		if (this.window.showProgressIndicator()) {
			if (this.window.animationItem != null) {
				Control progressWidget = this.window.animationItem.getControl();
				Rectangle bounds = this.window.animationItem.getImageBounds();
				int offset = 0;
				if (width > bounds.width)
					offset = (width - bounds.width) / 2;
				progressWidget.setBounds(
					offset,
					clientArea.y + clientArea.height - bounds.height,
					width,
					bounds.height);
				width = Math.max(width, bounds.width);

			}
		}

		if (this.window.getStatusLineManager() != null) {
			Control statusLine = this.window.getStatusLineManager().getControl();
			if (statusLine != null) {
				if (this.window.getShowStatusLine()) {

					if (this.window.getShortcutBar() != null && this.window.getShowShortcutBar()) {
						Widget shortcutBar = this.window.getShortcutBar().getControl();
						if (shortcutBar != null
							&& shortcutBar instanceof ToolBar) {
							ToolBar bar = (ToolBar) shortcutBar;
							if (bar.getItemCount() > 0) {
								ToolItem item = bar.getItem(0);
								width = Math.max(width, item.getWidth());
								Rectangle trim =
									bar.computeTrim(0, 0, width, width);
								width = trim.width;
							}
						}
					}

					Point statusLineSize =
						statusLine.computeSize(
							SWT.DEFAULT,
							SWT.DEFAULT,
							flushCache);
					statusLine.setBounds(
						clientArea.x + width,
						clientArea.y + clientArea.height - statusLineSize.y,
						clientArea.width - width,
						statusLineSize.y);
					clientArea.height -= statusLineSize.y + WorkbenchWindow.VGAP;
				} else
					this.window.getStatusLineManager().getControl().setBounds(
						0,
						0,
						0,
						0);
			}
		}

		if (this.window.getShortcutBar() != null) {
			Control shortCutBar = this.window.getShortcutBar().getControl();
			if (shortCutBar != null) {
				if (this.window.getShowShortcutBar()) {

					if (shortCutBar instanceof ToolBar) {
						ToolBar bar = (ToolBar) shortCutBar;
						if (bar.getItemCount() > 0) {
							ToolItem item = bar.getItem(0);
							width = item.getWidth();
							Rectangle trim =
								bar.computeTrim(0, 0, width, width);
							width = trim.width;
						}
					}
					shortCutBar.setBounds(
						clientArea.x,
						clientArea.y,
						width,
						clientArea.height);
					clientArea.x += width + WorkbenchWindow.VGAP;
					clientArea.width -= width + WorkbenchWindow.VGAP;

			}
		}

		} else
			this.window.getShortcutBar().getControl().setBounds(0, 0, 0, 0);

		Control sep3 = this.window.getSeparator3();

		if (sep3 != null) {
			if (this.window.getShowShortcutBar()) {
				Point sep3Size =
					sep3.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				sep3.setBounds(
					clientArea.x,
					clientArea.y,
					sep3Size.x,
					clientArea.height);
				clientArea.x += sep3Size.x;
			} else
				sep3.setBounds(0, 0, 0, 0);
		}

		if (this.window.getClientComposite() != null)
			this.window.getClientComposite().setBounds(
				clientArea.x + WorkbenchWindow.CLIENT_INSET,
				clientArea.y + WorkbenchWindow.CLIENT_INSET + WorkbenchWindow.VGAP,
				clientArea.width - (2 * WorkbenchWindow.CLIENT_INSET),
				clientArea.height - WorkbenchWindow.VGAP - (2 * WorkbenchWindow.CLIENT_INSET));

	}
}