
package neo.reducecognitivecomplexity.sonar;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Paging {

	@SerializedName("pageIndex")
	@Expose
	private Integer pageIndex;
	@SerializedName("pageSize")
	@Expose
	private Integer pageSize;
	@SerializedName("total")
	@Expose
	private Integer total;

	public Integer getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(Integer pageIndex) {
		this.pageIndex = pageIndex;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

}
