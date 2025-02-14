/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.BundleException;

/**
 * Test supposed to be used interactively to monitor the error message output.
 */
public class TransferExceptionsTest extends AbstractProvisioningTest {

	public void testErrorMessages() throws IOException, BundleException, URISyntaxException {
		File f = File.createTempFile("TransferTest", "dummy.txt");
		try (OutputStream fos = Files.newOutputStream(f.toPath())) {
			Platform.getBundle("org.eclipse.ecf.provider.filetransfer").start();
			IStatus s = getTransport().download(new URI("bogus!bogus"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("1", s);
			s = getTransport().download(new URI("bogus://somewhere.else"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("2", s);
			s = getTransport().download(new URI("http:bogusURL"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("3", s);
			s = getTransport().download(new URI("http://bogusURL:80/"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("4", s);
			s = getTransport().download(new URI("http:/bogusURL:999999999999/"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("5", s);
			s = getTransport().download(new URI("http://bogus.nowhere"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("6", s);
			s = getTransport().download(new URI("http://www.eclipse.org/AFileThatDoesNotExist.foo"), fos,
					new NullProgressMonitor());
			assertNotOK(s);
			printStatus("7", s);
		}
	}

	private static void printStatus(String msg, IStatus s) {
		System.err.print("TEST OUTPUT: " + msg + "\n");
		System.err.print("     ");
		System.err.print("Message [" + s.getMessage() + "] Exception Class[" + s.getException().getClass().getName() + "] ExceptionMessage[ ");
		System.err.print(s.getException().getMessage() + "]\n");

	}

}
