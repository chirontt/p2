/*******************************************************************************
 * Copyright (c) 2015, 2017 Rapicorp, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;
import org.tukaani.xz.XZInputStream;

public class XZedSimpleMetadataRepositoryFactory extends MetadataRepositoryFactory {
	private static final String REPOSITORY_FILENAME = "content.xml.xz"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	@Override
	public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) {
		if (location.getScheme().equals("file")) { //$NON-NLS-1$
			return new LocalMetadataRepository(getAgent(), location, name, properties);
		}
		return new URLMetadataRepository(getAgent(), location, name, properties);
	}

	/**
	 * Returns a file in the local file system that contains the contents of the
	 * metadata repository at the given location.
	 */
	private File getLocalFile(URI location, IProgressMonitor monitor) throws IOException, ProvisionException {
		File localFile = null;
		URI xzLocation = URIUtil.append(location, REPOSITORY_FILENAME);
		// If the repository is local, we can return the repository file directly
		if (PROTOCOL_FILE.equals(xzLocation.getScheme())) {
			//look for a compressed local file
			localFile = URIUtil.toFile(xzLocation);
			if (localFile.exists()) {
				return localFile;
			}
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Constants.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
		// file is not local, create a cache of the repository metadata
		CacheManager cache = getAgent().getService(CacheManager.class);
		if (cache == null) {
			throw new IllegalArgumentException("Cache manager service not available"); //$NON-NLS-1$
		}
		localFile = cache.createCacheFromFile(URIUtil.append(location, REPOSITORY_FILENAME), monitor);
		if (localFile == null) {
			// there is no remote file in either form - this should not really happen as
			// createCache should bail out with exception if something is wrong. This is an internal
			// error.
			throw new ProvisionException(new Status(IStatus.ERROR, Constants.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.repoMan_internalError, null));
		}
		return localFile;
	}

	@Override
	public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		long time = 0;
		final String debugMsg = "Validating and loading metadata repository "; //$NON-NLS-1$
		if (Tracing.DEBUG_METADATA_PARSING) {
			Tracing.debug(debugMsg + location);
			time = -System.currentTimeMillis();
		}
		SubMonitor sub = SubMonitor.convert(monitor, 400);
		try {
			File localFile = getLocalFile(location, sub.newChild(300));
			InputStream stream = new BufferedInputStream(new FileInputStream(localFile));
			XZInputStream descriptorStream = new XZInputStream(stream);
			try {
				//parse the repository descriptor file
				sub.setWorkRemaining(100);
				IMetadataRepository result = new MetadataRepositoryIO(getAgent()).read(localFile.toURL(), descriptorStream, sub.newChild(100));
				if (result != null && (flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0 && !result.isModifiable()) {
					return null;
				}
				if (result instanceof LocalMetadataRepository) {
					((LocalMetadataRepository) result).initializeAfterLoad(location);
				}
				if (result instanceof URLMetadataRepository) {
					((URLMetadataRepository) result).initializeAfterLoad(location);
				}
				if (Tracing.DEBUG_METADATA_PARSING) {
					time += System.currentTimeMillis();
					Tracing.debug(debugMsg + "time (ms): " + time); //$NON-NLS-1$
				}
				return result;
			} finally {
				safeClose(descriptorStream);
				safeClose(stream);
			}
		} catch (FileNotFoundException e) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Constants.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Constants.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/**
	 * Closes a stream, ignoring any secondary exceptions
	 */
	private void safeClose(InputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException e) {
			//ignore
		}
	}
}
