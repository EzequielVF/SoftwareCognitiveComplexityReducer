package neo.reducecognitivecomplexity.sonar;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Analysis {

	@SerializedName("key")
	@Expose
	private String key;
	@SerializedName("date")
	@Expose
	private String date;
	@SerializedName("events")
	@Expose
	private List<Event> events = null;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

}