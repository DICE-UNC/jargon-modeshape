/**
 * 
 */
package org.irods.jargon.modeshape.connector.exceptions;

import org.irods.jargon.core.exception.JargonRuntimeException;

/**
 * Runtime exception for internal operations, including general Jargon and iRODS
 * exceptions that may occur and are otherwise not exposed in method signatures
 * 
 * @author Mike Conway - DICE
 * 
 */
public class IrodsConnectorRuntimeException extends JargonRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2033371443813738743L;

	/**
	 * 
	 */
	public IrodsConnectorRuntimeException() {
	}

	/**
	 * @param arg0
	 */
	public IrodsConnectorRuntimeException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public IrodsConnectorRuntimeException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public IrodsConnectorRuntimeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
