/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.InputStream;
import java.net.URL;

import javax.jcr.RepositoryException;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.binary.UrlBinaryValue;

/**
 * IRODS implemenetation of UrlBinaryValue
 * 
 * @author mikeconway
 * 
 */
public class IRODSBinaryValue extends UrlBinaryValue {

	private final ConnectorContext connectorContext;

	/**
	 * @param sha1
	 * @param sourceName
	 * @param content
	 * @param size
	 * @param nameHint
	 * @param mimeTypeDetector
	 */
	public IRODSBinaryValue(String sha1, String sourceName, URL content,
			long size, String nameHint, MimeTypeDetector mimeTypeDetector,
			ConnectorContext connectorContext) {
		super(sha1, sourceName, content, size, nameHint, mimeTypeDetector);
		this.connectorContext = connectorContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.UrlBinaryValue#getStream()
	 */
	@Override
	public InputStream getStream() throws RepositoryException {
		try {

			IRODSFileFactory irodsFileFactory = connectorContext
					.getIrodsAccessObjectFactory().getIRODSFileFactory(
							connectorContext.getProxyAccount());

			InputStream inputStream = irodsFileFactory
					.instanceSessionClosingIRODSFileInputStream(this.getId());
			return inputStream;

		} catch (JargonException e) {
			throw new RepositoryException(e);
		}
	}

}
