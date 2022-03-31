
package neo.reducecognitivecomplexity.sonar;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Issue {

	@SerializedName("key")
	@Expose
	private String key;
	@SerializedName("rule")
	@Expose
	private String rule;
	@SerializedName("severity")
	@Expose
	private String severity;
	@SerializedName("component")
	@Expose
	private String component;
	@SerializedName("project")
	@Expose
	private String project;
	@SerializedName("line")
	@Expose
	private Integer line;
	@SerializedName("hash")
	@Expose
	private String hash;
	@SerializedName("textRange")
	@Expose
	private TextRange textRange;
	@SerializedName("flows")
	@Expose
	private List<Flow> flows = null;
	@SerializedName("status")
	@Expose
	private String status;
	@SerializedName("message")
	@Expose
	private String message;
	@SerializedName("effort")
	@Expose
	private String effort;
	@SerializedName("debt")
	@Expose
	private String debt;
	@SerializedName("author")
	@Expose
	private String author;
	@SerializedName("tags")
	@Expose
	private List<String> tags = null;
	@SerializedName("creationDate")
	@Expose
	private String creationDate;
	@SerializedName("updateDate")
	@Expose
	private String updateDate;
	@SerializedName("type")
	@Expose
	private String type;
	@SerializedName("organization")
	@Expose
	private String organization;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public TextRange getTextRange() {
		return textRange;
	}

	public void setTextRange(TextRange textRange) {
		this.textRange = textRange;
	}

	public List<Flow> getFlows() {
		return flows;
	}

	public void setFlows(List<Flow> flows) {
		this.flows = flows;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getEffort() {
		return effort;
	}

	public void setEffort(String effort) {
		this.effort = effort;
	}

	public String getDebt() {
		return debt;
	}

	public void setDebt(String debt) {
		this.debt = debt;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

}
