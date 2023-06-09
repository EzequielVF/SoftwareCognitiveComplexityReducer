package neo.reducecognitivecomplexity.graphs;

import neo.reducecognitivecomplexity.algorithms.Pair;

/**
 * This class models a vertex of graphs that contain information about cognitive
 * complexity
 */
public class ExtractionVertex implements Comparable {
	private int initialOffset;
	private int endOffset;
	private int reductionOfCognitiveComplexity; //reduction of CC when extracting this vertex
	private int accumulatedInherentComponent; // accumulated inherent complexity of the current vertex and the ones contained by it
	private int accumulatedNestingComponent; // accumulated nesting complexity of the vertices contained by this vertex. The current vertex has nesting component 0.
	private int numberNestingContributors; // number of vertices that contribute to the nesting component of the current vertex (including itself when its nesting component is not 0)
	private int nesting; //nesting level (starting in 0)

	// Temporal values
	private Integer temporalAccumulatedInherentComponent;
	private Integer temporalAccumulatedNestingComponent;
	private Integer temporalNumberNestingContributors;

	public ExtractionVertex(int a, int b, int reductionOfCognitiveComplexity, int accumulatedInherentComponent,
			int accumulatedNestingComponent, int numberNestingContributors, int nesting) {
		this.initialOffset = a;
		this.endOffset = b;
		this.reductionOfCognitiveComplexity = reductionOfCognitiveComplexity;
		this.accumulatedInherentComponent = accumulatedInherentComponent;
		this.accumulatedNestingComponent = accumulatedNestingComponent;
		this.numberNestingContributors = numberNestingContributors;
		this.nesting = nesting;
	}

	public int getReductionOfCognitiveComplexity() {
		return this.reductionOfCognitiveComplexity;
	}

	public int getComplexityWhenExtracted() {
		return getAccumulatedInherentComponent() + getAccumulatedNestingComponent();
	}

	public int getAccumulatedInherentComponent() {
		if (temporalAccumulatedInherentComponent != null) {
			return temporalAccumulatedInherentComponent;
		}
		return accumulatedInherentComponent;
	}

	public int getAccumulatedNestingComponent() {
		if (temporalAccumulatedNestingComponent != null) {
			return temporalAccumulatedNestingComponent;
		}
		return accumulatedNestingComponent;
	}

	public int getNumberNestingContributors() {
		if (temporalNumberNestingContributors != null) {
			return temporalNumberNestingContributors;
		}
		return numberNestingContributors;
	}
	
	public int getInitialOffset() {
		return initialOffset;
	}
	
	public int getEndOffset() {
		return endOffset;
	}

	public void removeTemporalInformation() {
		temporalAccumulatedInherentComponent = null;
		temporalAccumulatedNestingComponent = null;
		temporalNumberNestingContributors = null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		return this.initialOffset == ((ExtractionVertex) obj).initialOffset
				&& this.endOffset == ((ExtractionVertex) obj).endOffset;
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(this.initialOffset + (this.endOffset - this.initialOffset));
	}

	@Override
	public String toString() {
		return "[" + initialOffset + ", " + endOffset + "] (" + reductionOfCognitiveComplexity + ", "
				+ accumulatedInherentComponent + ", " + accumulatedNestingComponent + ", " + numberNestingContributors
				+ ", " + nesting + ")";
	}

	public void setTemporalAccumulatedInherentComponent(Integer temporalAccumulatedInherentComponent) {
		this.temporalAccumulatedInherentComponent = temporalAccumulatedInherentComponent;
	}

	public void setTemporalAccumulatedNestingComponent(Integer temporalAccumulatedNestingComponent) {
		this.temporalAccumulatedNestingComponent = temporalAccumulatedNestingComponent;
	}

	public void setTemporalNumberNestingContributors(Integer temporalNumberNestingContributors) {
		this.temporalNumberNestingContributors = temporalNumberNestingContributors;
	}

	public int getNesting() {
		return nesting;
	}

	@Override
	public int compareTo(Object o) {
		Pair a = new Pair(this.initialOffset, this.endOffset);
		Pair b = new Pair(((ExtractionVertex)o).getInitialOffset(), ((ExtractionVertex)o).getEndOffset());
		
		return a.compareTo(b);
	}
}
