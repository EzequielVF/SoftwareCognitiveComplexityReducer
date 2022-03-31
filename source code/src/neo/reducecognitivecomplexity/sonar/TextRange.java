
package neo.reducecognitivecomplexity.sonar;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TextRange {

	@SerializedName("startLine")
	@Expose
	private Integer startLine;
	@SerializedName("endLine")
	@Expose
	private Integer endLine;
	@SerializedName("startOffset")
	@Expose
	private Integer startOffset;
	@SerializedName("endOffset")
	@Expose
	private Integer endOffset;

	public Integer getStartLine() {
		return startLine;
	}

	public void setStartLine(Integer startLine) {
		this.startLine = startLine;
	}

	public Integer getEndLine() {
		return endLine;
	}

	public void setEndLine(Integer endLine) {
		this.endLine = endLine;
	}

	public Integer getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(Integer startOffset) {
		this.startOffset = startOffset;
	}

	public Integer getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(Integer endOffset) {
		this.endOffset = endOffset;
	}

	@Override
	public String toString() {
		return new String("[" + startLine + "," + endLine + "] offset=[" + startOffset + "," + endOffset + "]");
	}
}
