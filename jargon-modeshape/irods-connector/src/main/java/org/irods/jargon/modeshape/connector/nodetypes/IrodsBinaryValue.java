/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.InputStream;
import java.net.URL;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.modeshape.connector.exceptions.IrodsConnectorRuntimeException;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.binary.UrlBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE
 * 
 */
public class IrodsBinaryValue extends UrlBinaryValue {

	private static final long serialVersionUID = -6051801202338230704L;

	public static final Logger log = LoggerFactory
			.getLogger(IrodsBinaryValue.class);

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
	public IrodsBinaryValue(final String sha1, final String sourceName,
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
	 * @see org.modeshape.jcr.value.binary.UrlBinaryValue#toUrl()
	 */
	@Override
	protected URL toUrl() {
		return super.toUrl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.UrlBinaryValue#internalStream()
	 */
	@Override
	protected InputStream internalStream() {
		log.info("getStream()");

		try {

			IRODSFileFactory irodsFileFactory = irodsAccessObjectFactory
					.getIRODSFileFactory(irodsAccount);

			log.info("getting input stream for id:{}", getId());

			InputStream inputStream = irodsFileFactory
					.instanceSessionClosingIRODSFileInputStream(getId());
			return inputStream;

		} catch (JargonException e) {
			log.error("JargonException getting stream", e);
			throw new IrodsConnectorRuntimeException(e);
		}
	}

}
