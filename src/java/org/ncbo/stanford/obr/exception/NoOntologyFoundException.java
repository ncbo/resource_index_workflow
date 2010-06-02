package org.ncbo.stanford.obr.exception;

public class NoOntologyFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public NoOntologyFoundException() {
		super("No new ontology found");
	}

}
