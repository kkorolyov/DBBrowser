package dev.kkorolyov.dbbrowser.exceptions;

public class NullParameterException extends Exception {
	private static final long serialVersionUID = 9085037412714827026L;

	public NullParameterException() {
		this(null);
	}
	public NullParameterException(String[] parameters) {
		super(buildMessage(parameters));
	}
	
	private static String buildMessage(String[] parameters) {
		String message = "Method does not support null parameters";
		
		if (parameters != null && parameters.length > 0) {
			message += " for ";
			for (int i = 0; i < parameters.length - 1; i++) {
				message += parameters[i] + ", ";
			}
			message += parameters[parameters.length - 1];
		}
		return message;
	}
}
