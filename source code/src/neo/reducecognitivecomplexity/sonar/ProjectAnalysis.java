package neo.reducecognitivecomplexity.sonar;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProjectAnalysis {

	@SerializedName("paging")
	@Expose
	private Paging paging;
	@SerializedName("analyses")
	@Expose
	private List<Analysis> analyses = null;

	public Paging getPaging() {
		return paging;
	}

	public void setPaging(Paging paging) {
		this.paging = paging;
	}

	public List<Analysis> getAnalyses() {
		return analyses;
	}

	public void setAnalyses(List<Analysis> analyses) {
		this.analyses = analyses;
	}
}
