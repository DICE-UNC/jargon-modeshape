/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.IOException;
import java.net.URL;

import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.UrlBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creator of content nodes (binary content)
 * 
 * @author Mike Conway - DICE
 * 
 */
public class ContentNodeCreator extends AbstractNodeTypeCreator {

	public static final Logger log = LoggerFactory
			.getLogger(ContentNodeCreator.class);

	private final FileFromIdConverter fileFromIdConverter;

	/**
	 * @param irodsAccessObjectFactory
	 * @param irodsAccount
	 * @param connector
	 */
	public ContentNodeCreator(
			IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount, IrodsWriteableConnector connector) {
		super(irodsAccessObjectFactory, irodsAccount, connector);
		this.fileFromIdConverter = new FileFromIdConverterImpl(
				irodsAccessObjectFactory, irodsAccount,
				connector.getPathUtilities());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #instanceForId(java.lang.String, int)
	 */
	@Override
	public Document instanceForId(String id, int offset)
			throws RepositoryException {
		log.info("instanceForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}
		log.info("id:{}", id);
		log.info("offset:{}", offset);

		// trim the /jcr:content suffix
		String trimmedId = id.substring(0, id.length()
				- PathUtilities.JCR_CONTENT_SUFFIX_LENGTH);
		IRODSFile file;
		try {
			file = fileFromIdConverter.fileFor(trimmedId);
		} catch (JargonException e) {
			log.error("error getting file from id", e);
			throw new RepositoryException("error getting file", e);
		}
		if (this.getPathUtilities().isExcluded(file)) {
			log.info("file is excluded by filter or does not exist..return null");
			return null;
		}

		DocumentWriter writer = newDocument(trimmedId);
		BinaryValue binaryValue = createBinaryValue(file);
		writer.setPrimaryType(PathUtilities.NT_RESOURCE);
		writer.addProperty(PathUtilities.JCR_DATA, binaryValue);
		if (this.getConnector().isAddMimeTypeMixin()) {
			String mimeType = null;
			String encoding = null; // We don't really know this
			try {
				mimeType = binaryValue.getMimeType();
			} catch (IOException e) {
				log.error("io exception getting mime type", e);
				throw new RepositoryException("io exception getting mime type",
						e);
			}

			writer.addProperty(PathUtilities.JCR_ENCODING, encoding);
			writer.addProperty(PathUtilities.JCR_MIME_TYPE, mimeType);
		}
		writer.addProperty(PathUtilities.JCR_LAST_MODIFIED, factories()
				.getDateFactory().create(file.lastModified()));
		writer.addProperty(PathUtilities.JCR_LAST_MODIFIED_BY, null); // ignored

		// make these binary not queryable. If we really want to query
		// them,
		// we need to switch to external binaries
		writer.setNotQueryable();

		if (!PathUtilities.isRoot(trimmedId)) {
			// Set the reference to the parent ...
			String parentId = this.getPathUtilities().idFor(file);
			writer.setParents(parentId);
		}

		// Add the 'mix:mixinType' mixin; if other mixins are stored in the
		// extra properties, this will append ...
		if (this.getConnector().isAddMimeTypeMixin()) {
			writer.addMixinType(MIX_MIME_TYPE);
		}

		// Return the document ...
		return writer.document();
	}

	/**
	 * Utility method to create a {@link BinaryValue} object for the given file.
	 * Subclasses should rarely override this method, since the
	 * {@link UrlBinaryValue} will be applicable in most situations.
	 * 
	 * @param file
	 *            the file for which the {@link BinaryValue} is to be created;
	 *            never null
	 * @return the binary value; never null
	 * @throws IOException
	 *             if there is an error creating the value
	 */
	protected ExternalBinaryValue createBinaryValue(final IRODSFile file) {

		log.info("createBinaryFile()");
		assert file != null;
		log.info("file:{}", file);

		URL content = null;
		try {
			content = this.getPathUtilities().createUrlForFile(file);
		} catch (IOException e) {
			log.error("IOException creating url from file", e);
		}
		return new IrodsBinaryValue(this.getPathUtilities().sha1(file), this
				.getConnector().getSourceName(), content, file.length(),
				file.getName(), this.getConnector().getMimeTypeDetector(),
				this.getIrodsAccessObjectFactory(), this.getIrodsAccount());
	}

}
