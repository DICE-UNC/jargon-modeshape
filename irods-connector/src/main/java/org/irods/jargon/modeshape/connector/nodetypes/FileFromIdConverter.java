package org.irods.jargon.modeshape.connector.nodetypes;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.io.IRODSFile;

/**
 * Interface for a converter to go from ModeShape ids to iRODS files
 * 
 * @author mikeconway
 * 
 */
public interface FileFromIdConverter {

	/**
	 * Given an ModeShape id, return the iRODS file
	 * 
	 * @param id
	 *            <code>String</code>
	 * 
	 * @return {@link IRODSFile} that is the iRODS file
	 * @throws JargonException
	 */
	IRODSFile fileFor(String id) throws JargonException;

}