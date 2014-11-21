/**
 *
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.service.AbstractJargonService;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facility to transliate modeshape ids to iRODS files
 * 
 * @author Mike Conway - DICE
 * 
 */
public class FileFromIdConverterImpl extends AbstractJargonService implements
		FileFromIdConverter {

	public static final Logger log = LoggerFactory
			.getLogger(FileFromIdConverterImpl.class);

	private final PathUtilities pathUtilities;

	/**
	 * Constructor with prereqs
	 * 
	 * @param irodsAccessObjectFactory
	 *            {@link IRODSAccessObjectFactory}
	 * @param irodsAccount
	 *            {@link IRODSAccount}
	 * @param pathUtilities
	 *            {@link PathUtilities}
	 */
	public FileFromIdConverterImpl(
			final IRODSAccessObjectFactory irodsAccessObjectFactory,
			final IRODSAccount irodsAccount, final PathUtilities pathUtilities) {
		super(irodsAccessObjectFactory, irodsAccount);

		if (pathUtilities == null) {
			throw new IllegalArgumentException("null pathUtilities");
		}

		this.pathUtilities = pathUtilities;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.FileFromIdConverter#fileFor
	 * (java.lang.String)
	 */
	@Override
	public IRODSFile fileFor(final String id) throws JargonException {
		log.info("fileFor()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		log.info("id:{}", id);

		NodeTypeAndId nodeTypeAndId = this.pathUtilities.stripSuffixFromId(id);

		String strippedId = nodeTypeAndId.getId();
		String parentPath = pathUtilities.getDirectoryPath();

		log.info("getting file for parent path:{}", parentPath);
		log.info("child path:{}", strippedId);

		return this.getIrodsAccessObjectFactory()
				.getIRODSFileFactory(getIrodsAccount())
				.instanceIRODSFile(parentPath, strippedId);
	}

}
