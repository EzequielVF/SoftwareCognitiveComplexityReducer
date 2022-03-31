package neo.reducecognitivecomplexity.sonar.cognitivecomplexity;

import java.util.List;

import neo.reducecognitivecomplexity.sonar.TextRange;

/**
 * Contains information extracted by SONAR about the complexity of a method.
 */
public class CognitiveComplexMethod {
	private String projectNameInSonar;
	private String file;
	private int complexity, complexityThreshold;
	private TextRange textRange;
	private List<Contribution> contributionToComplexity;

	/**
	 * 
	 * @param project
	 * @param file
	 * @param complexityThreshold
	 * @param complexity
	 * @param textRange
	 * @param contributionToComplexity
	 */
	public CognitiveComplexMethod(String project, String file, int complexityThreshold, int complexity,
			TextRange textRange, List<Contribution> contributionToComplexity) {
		setProjectNameInSonar(project);
		setComplexityThreshold(complexityThreshold);
		setComplexity(complexity);
		setFile(file);
		setTextRange(textRange);
		this.contributionToComplexity = contributionToComplexity;
	}

	public int getComplexity() {
		return complexity;
	}

	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}

	public int getComplexityThreshold() {
		return complexityThreshold;
	}

	public void setComplexityThreshold(int complexityThreshold) {
		this.complexityThreshold = complexityThreshold;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getProjectName() {
		return projectNameInSonar.substring(projectNameInSonar.lastIndexOf(":") + 1);
	}

	public String getProjectNameInSonar() {
		return projectNameInSonar;
	}

	public void setProjectNameInSonar(String project) {
		this.projectNameInSonar = project;
	}

	public TextRange getTextRange() {
		return textRange;
	}

	public void setTextRange(TextRange textRange) {
		this.textRange = textRange;
	}

	public List<Contribution> getContributionToComplexity() {
		return contributionToComplexity;
	}

	public void setContributionToComplexity(List<Contribution> contributionToComplexity) {
		this.contributionToComplexity = contributionToComplexity;
	}

	@Override
	public String toString() {
		return new String("project: [" + projectNameInSonar + "] file: [" + file + "] complexity: [" + complexity
				+ "] textRange: [" + textRange + "] nblocks contributing: [" + this.contributionToComplexity.size()
				+ "]");
	}
}
