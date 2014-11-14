/**
 * 
 */
package org.irods.jargon.modeshape.connector.exceptions;

import org.irods.jargon.core.exception.JargonException;

/**
 * Node type is unknown
 * 
 * @author Mike Conway - DICE
 * 
 */
public class UnknownNodeTypeException extends JargonException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1085710114531065916L;

	/**
	 * @param message
	 */
	public UnknownNodeTypeException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnknownNodeTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public UnknownNodeTypeException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param underlyingIRODSExceptionCode
	 */
	public UnknownNodeTypeException(String message, Throwable cause,
			int underlyingIRODSExceptionCode) {
		super(message, cause, underlyingIRODSExceptionCode);
	}

	/**
	 * @param cause
	 * @param underlyingIRODSExceptionCode
	 */
	public UnknownNodeTypeException(Throwable cause,
			int underlyingIRODSExceptionCode) {
		super(cause, underlyingIRODSExceptionCode);
	}

	/**
	 * @param message
	 * @param underlyingIRODSExceptionCode
	 */
	public UnknownNodeTypeException(String message,
			int underlyingIRODSExceptionCode) {
		super(message, underlyingIRODSExceptionCode);
	}

}
