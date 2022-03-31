
package neo.reducecognitivecomplexity.sonar;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Location {

	@SerializedName("component")
	@Expose
	private String component;
	@SerializedName("textRange")
	@Expose
	private TextRange textRange;
	@SerializedName("msg")
	@Expose
	private String msg;

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public TextRange getTextRange() {
		return textRange;
	}

	public void setTextRange(TextRange textRange) {
		this.textRange = textRange;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
