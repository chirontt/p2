/*******************************************************************************
 *  Copyright (c) 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.provisional.p2.directorywatcher.messages"; //$NON-NLS-1$
	public static String error_main_loop;
	public static String error_processing;
	public static String failed_create_repo;
	public static String filename_missing;
	public static String repo_manager_not_registered;
	public static String null_folder;
	public static String thread_not_started;
	public static String thread_started;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//
	}
}
