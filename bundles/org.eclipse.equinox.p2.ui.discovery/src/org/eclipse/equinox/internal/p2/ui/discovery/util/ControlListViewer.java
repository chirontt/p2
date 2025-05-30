/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.util;

import java.util.*;
import java.util.List;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Based on {@link org.eclipse.ui.internal.progress.DetailedProgressViewer}.
 */
@SuppressWarnings("restriction")
public abstract class ControlListViewer extends StructuredViewer {

	Composite control;

	private final ScrolledComposite scrolled;

	private final Composite noEntryArea;

	protected boolean hasFocus;

	/**
	 * Create a new instance of the receiver with a control that is a child of
	 * parent with style style.
	 */
	public ControlListViewer(Composite parent, int style) {
		scrolled = new ScrolledComposite(parent, style | SWT.VERTICAL);
		int height = JFaceResources.getDefaultFont().getFontData()[0].getHeight();
		scrolled.getVerticalBar().setIncrement(height * 2);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		// bug 311276: can cause unintended scrolling of viewer
		// scrolled.setShowFocusedControl(true);

		control = new Composite(scrolled, SWT.NONE) {
			// @Override
			// public boolean setFocus() {
			// forceFocus();
			// return true;
			// }

			@Override
			public void setVisible(boolean visible) {
				super.setVisible(visible);
				if (visible) {
					updateSize(control);
				}
			}
		};
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		control.setLayout(layout);
		control.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		control.setBackgroundMode(SWT.INHERIT_FORCE);
		control.addControlListener(new ControlListener() {
			@Override
			public void controlMoved(ControlEvent e) {
				updateVisibleItems();
			}

			@Override
			public void controlResized(ControlEvent e) {
				updateVisibleItems();
			}
		});

		scrolled.setContent(control);
		hookControl(control);

		noEntryArea = new Composite(scrolled, SWT.NONE);
		doCreateNoEntryArea(noEntryArea);

		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.addControlListener(ControlListener.controlResizedAdapter(e -> updateSize(scrolled.getContent())));
		control.addTraverseListener(new TraverseListener() {
			private boolean handleEvent = true;

			@Override
			public void keyTraversed(TraverseEvent event) {
				if (!handleEvent) {
					return;
				}
				switch (event.detail) {
				case SWT.TRAVERSE_ARROW_PREVIOUS: {
					Control[] children = control.getChildren();
					if (children.length > 0) {
						boolean selected = false;
						for (int i = 0; i < children.length; i++) {
							ControlListItem<?> item = (ControlListItem<?>) children[i];
							if (item.isSelected()) {
								selected = true;
								if (i > 0) {
									setSelection(new StructuredSelection(children[i - 1].getData()), true);
								}
								break;
							}
						}
						if (!selected) {
							setSelection(new StructuredSelection(children[children.length - 1].getData()), true);
						}
					}
					break;
				}
				case SWT.TRAVERSE_ARROW_NEXT: {
					Control[] children = control.getChildren();
					if (children.length > 0) {
						boolean selected = false;
						for (int i = 0; i < children.length; i++) {
							ControlListItem<?> item = (ControlListItem<?>) children[i];
							if (item.isSelected()) {
								selected = true;
								if (i < children.length - 1) {
									setSelection(new StructuredSelection(children[i + 1].getData()), true);
								}
								break;
							}
						}
						if (!selected) {
							setSelection(new StructuredSelection(children[0].getData()), true);
						}
					}
					break;
				}
				default:
					handleEvent = false;
					event.doit = true;
					Control c = ControlListViewer.this.control;
					Shell shell = c.getShell();
					while (c != null) {
						if (c.traverse(event.detail)) {
							break;
						}
						if (!event.doit || c == shell) {
							break;
						}
						c = c.getParent();
					}
					handleEvent = true;
					break;
				}
			}
		});
	}

	protected void doCreateNoEntryArea(Composite parent) {
		//
	}

	public void add(Object[] elements) {
		ViewerComparator sorter = getComparator();

		// Use a Set in case we are getting something added that exists
		Set<Object> newItems = new HashSet<>(elements.length);

		Control[] existingChildren = control.getChildren();
		for (Control element : existingChildren) {
			if (element.getData() != null) {
				newItems.add(element.getData());
			}
		}

		for (Object element : elements) {
			if (element != null) {
				newItems.add(element);
			}
		}

		Object[] infos = new Object[newItems.size()];
		newItems.toArray(infos);

		if (sorter != null) {
			sorter.sort(this, infos);
		}

		// Update with the new elements to prevent flash
		for (Control element : existingChildren) {
			element.dispose();
		}

		for (int i = 0; i < infos.length; i++) {
			ControlListItem<?> item = createNewItem(infos[i]);
			item.updateColors(i);
		}

		control.layout(true);
		doUpdateContent();
	}

	private void updateSize(Control c) {
		if (c == null) {
			return;
		}
		// XXX need a small offset in case the list has a scroll bar
		Point size = c.computeSize(scrolled.getClientArea().width - 20, SWT.DEFAULT, true);
		c.setSize(size);
		scrolled.setMinSize(size);
	}

	protected void doUpdateContent() {
		if (control.getChildren().length > 0) {
			updateSize(control);
			scrolled.setContent(control);
		} else {
			updateSize(noEntryArea);
			scrolled.setContent(noEntryArea);
		}
	}

	/**
	 * Create a new item for info.
	 *
	 * @return ControlListItem
	 */
	private ControlListItem<?> createNewItem(Object element) {
		final ControlListItem<?> item = doCreateItem(control, element);
		// item.getChildren()[0].addPaintListener(new PaintListener() {
		// public void paintControl(PaintEvent e) {
		// if (hasFocus && item.isSelected()) {
		// Point size = item.getSize();
		// e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY));
		// e.gc.setLineDash(new int[] { 1, 2 });
		// e.gc.drawRoundRectangle(0, 0, size.x - 1, size.y - 1, 5, 5);
		// }
		// }
		// });
		item.setIndexListener(new ControlListItem.IndexListener() {
			@Override
			public void selectNext() {
				Control[] children = control.getChildren();
				for (int i = 0; i < children.length; i++) {
					if (item == children[i]) {
						if (i < children.length - 1) {
							setSelection(new StructuredSelection(children[i + 1].getData()));
						}
						break;
					}
				}
			}

			@Override
			public void selectPrevious() {
				Control[] children = control.getChildren();
				for (int i = 0; i < children.length; i++) {
					if (item == children[i]) {
						if (i > 0) {
							setSelection(new StructuredSelection(children[i - 1].getData()));
						}
						break;
					}
				}
			}

			@Override
			public void select() {
				setSelection(new StructuredSelection(item.getData()));
				setFocus();
			}

			@Override
			public void open() {
				handleOpen();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(item);
		// Refresh to populate with the current tasks
		item.refresh();
		return item;
	}

	protected abstract ControlListItem<?> doCreateItem(Composite parent, Object element);

	@Override
	protected ControlListItem<?> doFindInputItem(Object element) {
		return null;
	}

	@Override
	protected ControlListItem<?> doFindItem(Object element) {
		Control[] children = control.getChildren();
		for (Control child : children) {
			if (child.isDisposed() || child.getData() == null) {
				continue;
			}
			if (child.getData().equals(element)) {
				return (ControlListItem<?>) child;
			}
		}
		return null;
	}

	@Override
	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
		if (usingElementMap()) {
			unmapElement(item);
		}
		item.dispose();
		add(new Object[] { element });
	}

	@Override
	public ScrolledComposite getControl() {
		return scrolled;
	}

	@Override
	protected List<?> getSelectionFromWidget() {
		Control[] children = control.getChildren();
		ArrayList<Object> selection = new ArrayList<>(children.length);
		for (Control child : children) {
			ControlListItem<?> item = (ControlListItem<?>) child;
			if (item.isSelected() && item.getData() != null) {
				selection.add(item.getData());
			}
		}
		return selection;
	}

	protected void handleOpen() {
		Control c = getControl();
		if (c != null && !c.isDisposed()) {
			ISelection selection = getSelection();
			fireOpen(new OpenEvent(this, selection));
		}
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
		refreshAll();
		doUpdateContent();
	}

	@Override
	protected void internalRefresh(Object element) {
		if (element == null) {
			return;
		}

		if (element.equals(getRoot())) {
			refreshAll();
			return;
		}
		Widget widget = findItem(element);
		if (widget == null) {
			add(new Object[] { element });
			return;
		}
		((ControlListItem<?>) widget).refresh();

		updateSize(control);
	}

	public void remove(Object[] elements) {
		for (Object element : elements) {
			Widget item = doFindItem(element);
			if (item != null) {
				unmapElement(element);
				item.dispose();
			}
		}

		Control[] existingChildren = control.getChildren();
		for (int i = 0; i < existingChildren.length; i++) {
			ControlListItem<?> item = (ControlListItem<?>) existingChildren[i];
			item.updateColors(i);
		}
		control.layout(true);
		doUpdateContent();
	}

	@Override
	public void reveal(Object element) {
		Control c = doFindItem(element);
		if (c != null) {
			revealControl(c);
		}
	}

	private void revealControl(Control c) {
		Rectangle clientArea = scrolled.getClientArea();
		Point origin = scrolled.getOrigin();
		Point location = c.getLocation();
		Point size = c.getSize();
		if (location.y + size.y > origin.y + clientArea.height) {
			scrolled.setOrigin(origin.x, location.y + size.y - clientArea.height);
		}
		if (location.y < origin.y) {
			scrolled.setOrigin(origin.x, location.y);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected void setSelectionToWidget(List list, boolean reveal) {
		if (list != null) {
			// see bug 423628. This should be suppressed as:
			// 1. it will cause warning at build time.
			// 2. should be possible to fix once swt/jface enables generics for good.
			@SuppressWarnings("unchecked")
			HashSet<Object> elements = new HashSet<>(list);
			Control[] children = control.getChildren();
			for (Control c : children) {
				ControlListItem child = (ControlListItem) c;
				boolean selected = elements.contains(child.getData());
				if (selected != child.isSelected()) {
					child.setSelected(selected);
				}
				if (reveal && selected) {
					revealControl(child);
					reveal = false;
				}
			}
		} else {
			Control[] children = control.getChildren();
			for (Control c : children) {
				ControlListItem child = (ControlListItem) c;
				if (child.isSelected()) {
					child.setSelected(false);
				}
			}
		}
	}

	/**
	 * Set focus on the current selection.
	 */
	public void setFocus() {
		Control[] children = control.getChildren();
		if (children.length > 0) {
			// causes the item's tool bar to get focus when clicked which is undesirable
			// for (Control element : children) {
			// ControlListItem item = (ControlListItem) element;
			// if (item.isSelected()) {
			// if (item.setFocus()) {
			// return;
			// }
			// }
			// }
			control.forceFocus();
		} else {
			noEntryArea.setFocus();
		}
	}

	/**
	 * Refresh everything as the root is being refreshed.
	 */
	private void refreshAll() {
		Object[] infos = getSortedChildren(getRoot());
		Control[] existingChildren = control.getChildren();

		for (Control element : existingChildren) {
			element.dispose();
		}

		for (int i = 0; i < infos.length; i++) {
			ControlListItem<?> item = createNewItem(infos[i]);
			item.updateColors(i);
		}

		control.layout(true);
		doUpdateContent();
	}

	/**
	 * Set the virtual items to be visible or not depending on the displayed area.
	 */
	private void updateVisibleItems() {
		Control[] children = control.getChildren();
		int top = scrolled.getOrigin().y;
		int bottom = top + scrolled.getParent().getBounds().height;
		for (Control element : children) {
			ControlListItem<?> item = (ControlListItem<?>) element;
			item.setDisplayed(top, bottom);
		}
	}

}
