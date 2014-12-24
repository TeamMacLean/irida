package ca.corefacility.bioinformatics.irida.exceptions;

/**
 * Exception for when there is an issue building a particular {@link Analysis} type.
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 *
 */
public class IridaWorkflowAnalysisTypeException extends IridaWorkflowException {
	
	private static final long serialVersionUID = 4801881701203933579L;

	/**
	 * Constructs a new {@link IridaWorkflowAnalysisTypeException} with the given message
	 * and cause.
	 * 
	 * @param message
	 *            The message explaining the error.
	 * @param cause
	 *            The cause of this message.
	 */
	public IridaWorkflowAnalysisTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new {@link IridaWorkflowAnalysisTypeException} with the given message.
	 * 
	 * @param message
	 *            The message explaining the error.
	 */
	public IridaWorkflowAnalysisTypeException(String message) {
		super(message);
	}
}
