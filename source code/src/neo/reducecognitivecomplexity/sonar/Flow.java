
package neo.reducecognitivecomplexity.sonar;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Flow {

	@SerializedName("locations")
	@Expose
	private List<Location> locations = null;

	public List<Location> getLocations() {
		return locations;
	}

	public void setLocations(List<Location> locations) {
		this.locations = locations;
	}

}
