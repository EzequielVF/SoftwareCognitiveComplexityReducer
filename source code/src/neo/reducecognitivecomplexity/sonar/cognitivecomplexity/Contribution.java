package neo.reducecognitivecomplexity.sonar.cognitivecomplexity;

import neo.reducecognitivecomplexity.sonar.TextRange;

/**
 * Contains information extracted by SONAR about the contribution to the
 * cognitive complexity of a method for a block of code.
 */
public class Contribution {
	int contribution;
	String message;
	TextRange textRange;

	public Contribution(int contribution, String message, TextRange textRange) {
		setContribution(contribution);
		setMessage(message);
		setTextRange(textRange);
	}

	public int getContribution() {
		return contribution;
	}

	public void setContribution(int contribution) {
		this.contribution = contribution;
	}

	public int getNesting() {
		int result;

		if (message.contains("(")) {
			result = Integer.parseInt(message.substring(message.indexOf("incl ") + 5, message.indexOf(" for")));
		} else {
			result = 0;
		}

		return result;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public TextRange getTextRange() {
		return textRange;
	}

	public void setTextRange(TextRange textRange) {
		this.textRange = textRange;
	}

	@Override
	public String toString() {
		return new String(
				"contribution: [" + contribution + "] message: [" + message + "] textRange: [" + textRange + "]");
	}
}
