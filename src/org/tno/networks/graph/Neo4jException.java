package org.tno.networks.graph;

public class Neo4jException extends Exception {

	public Neo4jException() {
	}

	public Neo4jException(String message) {
		super(message);
	}

	public Neo4jException(Throwable cause) {
		super(cause);
	}

	public Neo4jException(String message, Throwable cause) {
		super(message, cause);
	}

	public Neo4jException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
