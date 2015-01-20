/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.InputStream;
import java.net.URL;

import javax.jcr.RepositoryException;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.binary.UrlBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IRODS implemenetation of UrlBinaryValue
 * 
 * @author mikeconway
 * 
 */
public class IRODSBinaryValue extends UrlBinaryValue {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2488110109717325030L;

	public static final Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnector.class);

	private final IRODSAccount irodsAccount;
	private final IRODSAccessObjectFactory irodsAccessObjectFactory;

	/**
	 * @param sha1
	 * @param sourceName
	 * @param content
	 * @param size
	 * @param nameHint
	 * @param mimeTypeDetector
	 */
	public IRODSBinaryValue(final String sha1, final String sourceName,
			final URL content, final long size, final String nameHint,
			final MimeTypeDetector mimeTypeDetector,
			final IRODSAccessObjectFactory irodsAccessObjectFactory,
			final IRODSAccount irodsAccount) {

		super(sha1, sourceName, content, size, nameHint, mimeTypeDetector);
		this.irodsAccount = irodsAccount;
		this.irodsAccessObjectFactory = irodsAccessObjectFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.UrlBinaryValue#getStream()
	 */
	@Override
	public InputStream getStream() throws RepositoryException {

		log.info("getStream()");

		try {

			IRODSFileFactory irodsFileFactory = irodsAccessObjectFactory
					.getIRODSFileFactory(irodsAccount);

			log.info("getting input stream for id:{}", getId());

			String formattedId = getId().replaceAll("file:", "");

			log.info("formatted:{}", formattedId);

			InputStream inputStream = irodsFileFactory
					.instanceSessionClosingIRODSFileInputStream(formattedId);
			return inputStream;

		} catch (JargonException e) {
			throw new RepositoryException(e);
		}
	}

}
