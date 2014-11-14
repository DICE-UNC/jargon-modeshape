/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a file node (iRODS file) based in its id
 * 
 * @author Mike Conway - DICE
 * 
 */
public class FileNodeCreator extends AbstractNodeTypeCreator {

	public static final Logger log = LoggerFactory
			.getLogger(FileNodeCreator.class);

	public static final int JCR_CONTENT_SUFFIX_LENGTH = PathUtilities.JCR_CONTENT_SUFFIX
			.length();

	/**
	 * @param irodsAccessObjectFactory
	 * @param irodsAccount
	 * @param pathUtilities
	 */
	public FileNodeCreator(IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount, PathUtilities pathUtilities) {
		super(irodsAccessObjectFactory, irodsAccount, pathUtilities);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #instanceForId(java.lang.String)
	 */
	@Override
	public Document instanceForId(String id) throws JargonException {
		log.info("instanceForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}
		log.info("id:{}", id);
		DocumentWriter writer = null;

		File file = fileFor(id);
		if (this.getPathUtilities().isExcluded(file)) {
			log.info("file is excluded by filter or does not exist..return null");
			return null;
		}
		File parentFile = file.getParentFile();
		log.info("see if this is a data object or collection and act appropriately");

		if (file.isFile()) {
			return instanceForIdAsFile(id, file);
		} else {
			return instanceForIdAsCollection(id, file);
		}

	}

	private Document instanceForIdAsCollection(String id, File file) {
		return null;
	}

	private Document instanceForIdAsFile(String id, File file) {
		return null;
	}

	/**
	 * Given an id in ModeShape terms, return the corresponding iRODS file
	 * 
	 * @param id
	 *            <code>String</code> with the ModeShape id
	 * @return {@link File} that is the iRODS file
	 * @throws JargonException
	 */
	private File fileFor(String id) throws JargonException {
		log.info("fileFor()");
		log.info("id:{}", id);

		String strippedId = id.substring(0, id.length()
				- PathUtilities.DELIMITER_LENGTH);

		return (File) this
				.getIrodsAccessObjectFactory()
				.getIRODSFileFactory(getIrodsAccount())
				.instanceIRODSFile(
						this.getPathUtilities()
								.getDirectoryPathWithTrailingSlash(),
						strippedId);

	}
}
