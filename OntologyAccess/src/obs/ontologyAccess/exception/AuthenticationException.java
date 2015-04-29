/**
 * 
 */
package obs.ontologyAccess.exception;

/**
 * @author Michael Dorf
 *
 */
public class AuthenticationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7923252990689899804L;

	public static final String DEFAULT_MESSAGE = "Invalid credentials supplied";
	
	/**
	 * 
	 */
	public AuthenticationException() {
		this(DEFAULT_MESSAGE);
	}

	/**
	 * @param msg
	 */
	public AuthenticationException(String msg) {
		super(msg);
	}

	/**
	 * @param arg0
	 */
	public AuthenticationException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public AuthenticationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
