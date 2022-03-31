package neo.reducecognitivecomplexity.sonar;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Event {

	@SerializedName("key")
	@Expose
	private String key;
	@SerializedName("category")
	@Expose
	private String category;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("description")
	@Expose
	private String description;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
