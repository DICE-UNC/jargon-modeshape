/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.modeshape.connector.exceptions.IrodsConnectorRuntimeException;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE
 * 
 */
public class IrodsBinaryValue extends ExternalBinaryValue {

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
	public IrodsBinaryValue(final String sha1Key, final String sourceName,
			final String id, final long size, final String nameHint,
			final MimeTypeDetector mimeTypeDetector,
			final IRODSAccessObjectFactory irodsAccessObjectFactory,
			final IRODSAccount irodsAccount) {

		super(new BinaryKey(sha1Key), sourceName, id, size, nameHint,
				mimeTypeDetector);
		this.irodsAccount = irodsAccount;
		this.irodsAccessObjectFactory = irodsAccessObjectFactory;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.ExternalBinaryValue#getId()
	 */
	@Override
	public String getId() {
		log.debug("getId()");
		return super.getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.ExternalBinaryValue#getSourceName()
	 */
	@Override
	public String getSourceName() {
		log.debug("getSourceName()");
		return super.getSourceName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.value.binary.ExternalBinaryValue#setMimeType(java.lang
	 * .String)
	 */
	@Override
	protected void setMimeType(String mimeType) {
		log.debug("setMimeType(): {}", mimeType);
		super.setMimeType(mimeType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.ExternalBinaryValue#hasMimeType()
	 */
	@Override
	protected boolean hasMimeType() {
		return super.hasMimeType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.ExternalBinaryValue#getMimeType()
	 */
	@Override
	public String getMimeType() {
		return super.getMimeType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.value.binary.ExternalBinaryValue#getMimeType(java.lang
	 * .String)
	 */
	@Override
	public String getMimeType(String name) {
		return super.getMimeType(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.ExternalBinaryValue#getSize()
	 */
	@Override
	public long getSize() {
		log.debug("getSize()");
		return super.getSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.ExternalBinaryValue#toString()
	 */
	@Override
	public String toString() {
		return super.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.AbstractBinary#getKey()
	 */
	@Override
	public BinaryKey getKey() {
		log.debug("getKey()");
		log.debug("key is:{}", super.getKey());
		return super.getKey();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.AbstractBinary#hashCode()
	 */
	@Override
	public int hashCode() {
		log.debug("hashCode()");
		return super.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.AbstractBinary#getReadableSize()
	 */
	@Override
	public String getReadableSize() {
		log.debug("getReadableSize()");
		log.debug("readableSize:{}", super.getReadableSize());
		return super.getReadableSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.value.binary.AbstractBinary#getStream()
	 */
	@Override
	public InputStream getStream() throws RepositoryException {
		log.debug("getStream()");
		return super.getStream();
	}

}
