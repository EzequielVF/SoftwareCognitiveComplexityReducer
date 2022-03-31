package neo.reducecognitivecomplexity.sonar.cognitivecomplexity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import neo.reducecognitivecomplexity.sonar.Flow;
import neo.reducecognitivecomplexity.sonar.Issue;
import neo.reducecognitivecomplexity.sonar.Paging;
import neo.reducecognitivecomplexity.sonar.TextRange;

public class ProjectIssues {

	@SerializedName("total")
	@Expose
	private Integer total;
	@SerializedName("p")
	@Expose
	private Integer p;
	@SerializedName("ps")
	@Expose
	private Integer ps;
	@SerializedName("paging")
	@Expose
	private Paging paging;
	@SerializedName("issues")
	@Expose
	private List<Issue> issues = null;

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public Integer getP() {
		return p;
	}

	public void setP(Integer p) {
		this.p = p;
	}

	public Integer getPs() {
		return ps;
	}

	public void setPs(Integer ps) {
		this.ps = ps;
	}

	public Paging getPaging() {
		return paging;
	}

	public void setPaging(Paging paging) {
		this.paging = paging;
	}

	public List<Issue> getIssues() {
		return issues;
	}

	public void setIssues(List<Issue> issues) {
		this.issues = issues;
	}

	public static Map<String, List<CognitiveComplexMethod>> getCognitiveComplexity(ProjectIssues issues) {
		int contributionValue;
		int complexity, complexityThreshold;
		String project;
		String file;
		String msg;
		TextRange textRange;
		CognitiveComplexMethod complexMethod;

		Map<String, List<CognitiveComplexMethod>> result = new TreeMap<String, List<CognitiveComplexMethod>>();

		for (Issue issue : issues.getIssues()) {
			ArrayList<Contribution> contributionToMethod = new ArrayList<Contribution>();
			for (Flow flow : issue.getFlows()) {
				msg = flow.getLocations().get(0).getMsg();
				if (msg.contains("(")) {
					contributionValue = Integer.parseInt(msg.substring(1, msg.indexOf('(') - 1));
				} else {
					contributionValue = Integer.parseInt(msg.substring(1));
				}

				textRange = flow.getLocations().get(0).getTextRange();

				Contribution contribution = new Contribution(contributionValue, msg, textRange);
				contributionToMethod.add(contribution);
			}

			msg = issue.getMessage();
			complexity = Integer.parseInt(msg.substring(msg.indexOf("from") + 5, msg.indexOf(" to the")));
			complexityThreshold = Integer.parseInt(msg.substring(msg.indexOf("to the") + 7, msg.indexOf(" allowed")));
			project = issue.getComponent().substring(0, issue.getComponent().lastIndexOf(':'));
			file = issue.getComponent().substring(issue.getComponent().lastIndexOf(':') + 1);
			complexMethod = new CognitiveComplexMethod(project, file, complexityThreshold, complexity,
					issue.getTextRange(), contributionToMethod);

			if (!result.containsKey(file)) {
				result.put(file, new ArrayList<CognitiveComplexMethod>());
			}
			result.get(file).add(0, complexMethod);
		}

		return result;
	}
}
