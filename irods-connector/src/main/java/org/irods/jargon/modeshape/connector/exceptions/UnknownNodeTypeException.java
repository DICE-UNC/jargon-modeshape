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
	public UnknownNodeTypeException(final String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnknownNodeTypeException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public UnknownNodeTypeException(final Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param underlyingIRODSExceptionCode
	 */
	public UnknownNodeTypeException(final String message,
			final Throwable cause, final int underlyingIRODSExceptionCode) {
		super(message, cause, underlyingIRODSExceptionCode);
	}

	/**
	 * @param cause
	 * @param underlyingIRODSExceptionCode
	 */
	public UnknownNodeTypeException(final Throwable cause,
			final int underlyingIRODSExceptionCode) {
		super(cause, underlyingIRODSExceptionCode);
	}

	/**
	 * @param message
	 * @param underlyingIRODSExceptionCode
	 */
	public UnknownNodeTypeException(final String message,
			final int underlyingIRODSExceptionCode) {
		super(message, underlyingIRODSExceptionCode);
	}

}
