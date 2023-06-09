package neo.reducecognitivecomplexity.jdt;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.ltk.core.refactoring.Change;

/**
 * Contain metrics for a code extraction.
 * 
 * <p>
 * {@code feasible} true if a code extraction is feasible or not.
 * 
 * <p>
 * {@code reason} the reason why a code extraction is not valid (if it is
 * infeasible).
 * 
 * <p>
 * {@code applied} true if a code extraction is applied.
 * 
 * <p>
 * {@code numberOfExtractedLinesOfCode} length of the code extraction (in number
 * of lines).
 * 
 * <p>
 * {@code numberOfParametersInExtractedMethod} number of parameters of the
 * extracted method.
 * 
 * <p>
 * {@code reductionOfCognitiveComplexity} reduction of cognitive complexity in
 * method after extraction.
 * 
 * <p>
 * {@code accumulatedInherentComponent} Accumulated inherent component for
 * cognitive complexity.
 * 
 * <p>
 * {@code accumulatedNestingComponent} Accumulated nesting component for
 * cognitive complexity.
 * 
 * <p>
 * {@code numberNestingContributors} Number of nesting contributors for
 * cognitive complexity.
 * 
 * <p>
 * {@code nesting} Code complexity nesting.
 * 
 * <p>
 * {@code changes} Changes to apply (or applied) to extract code. The list
 * stores changes in the order they would be/were applied. Usually, a code
 * extraction involves just one change. We store this as a list to allow a
 * sequence of code extractions.
 * 
 * <p>
 * {@code undoChanges} Changes to undo a extract method refactoring if the code
 * extraction was feasible and applied. The list stores undo changes in the
 * order they would be/were applied. Usually, a code extraction involves just
 * one change. We store this as a list to allow a sequence of code extractions.
 */
public class CodeExtractionMetrics {
	/**
	 * If a code extraction is feasible or not.
	 */
	boolean feasible;

	/**
	 * If a code extraction code is not feasible, the reason why. "OK" is the
	 * extraction is feasible.
	 */
	String reason;

	/**
	 * If a code extraction was applied.
	 */
	boolean applied;

	/**
	 * Length (in lines of code) of the code extraction.
	 */
	int numberOfExtractedLinesOfCode;

	/**
	 * Number of parameters of the extracted method.
	 */
	int numberOfParametersInExtractedMethod;

	/**
	 * Reduction of cognitive complexity in method after extraction.
	 */
	int reductionOfCognitiveComplexity;

	/**
	 * Accumulated inherent component for cognitive complexity.
	 */
	int accumulatedInherentComponent;

	/**
	 * Accumulated nesting component for cognitive complexity.
	 */
	int accumulatedNestingComponent;

	/**
	 * Number of nesting contributors for cognitive complexity.
	 */
	int numberNestingContributors;

	/**
	 * Code complexity nesting.
	 */
	int nesting;

	/**
	 * Changes to apply (or applied) to extract code. The list stores changes in the
	 * order they would be/were applied. Usually, a code extraction involves just
	 * one change. We store this as a list to allow a sequence of code extractions.
	 */
	List<Change> changes = new ArrayList<Change>();

	/**
	 * Changes to undo a extract method refactoring if the code extraction was
	 * feasible and applied. The list stores undo changes in the order they would
	 * be/were applied. Usually, a code extraction involves just
	 * one change. We store this as a list to allow a sequence of code extractions.
	 */
	List<Change> undoChanges = new ArrayList<Change>();

	public CodeExtractionMetrics(boolean feasible, String reason, boolean applied, int numberOfExtractedLinesOfCode,
			int numberOfParametersInExtractedMethod, List<Change> changes, List<Change> undoChanges) {
		this.feasible = feasible;
		this.reason = reason;
		this.applied = applied;
		this.numberOfExtractedLinesOfCode = numberOfExtractedLinesOfCode;
		this.numberOfParametersInExtractedMethod = numberOfParametersInExtractedMethod;
		this.changes = changes;
		this.undoChanges = undoChanges;
		this.reductionOfCognitiveComplexity = 0;
		this.accumulatedInherentComponent = 0;
		this.accumulatedNestingComponent = 0;
		this.numberNestingContributors = 0;
		this.nesting = 0;
	}

	public CodeExtractionMetrics(boolean feasible, String reason, boolean applied, int numberOfExtractedLinesOfCode,
			int numberOfParametersInExtractedMethod, List<Change> changes, List<Change> undoChanges,
			int reductionOfCognitiveComplexity, int accumulatedInherentComponent, int accumulatedNestingComponent,
			int numberNestingContributors, int nesting) {
		this.feasible = feasible;
		this.reason = reason;
		this.applied = applied;
		this.numberOfExtractedLinesOfCode = numberOfExtractedLinesOfCode;
		this.numberOfParametersInExtractedMethod = numberOfParametersInExtractedMethod;
		this.changes = changes;
		this.undoChanges = undoChanges;
		this.reductionOfCognitiveComplexity = reductionOfCognitiveComplexity;
		this.accumulatedInherentComponent = accumulatedInherentComponent;
		this.accumulatedNestingComponent = accumulatedNestingComponent;
		this.numberNestingContributors = numberNestingContributors;
		this.nesting = nesting;
	}

	public CodeExtractionMetrics(CodeExtractionMetrics metrics) {
		this.feasible = metrics.feasible;
		this.reason = metrics.reason;
		this.applied = metrics.applied;
		this.numberOfExtractedLinesOfCode = metrics.numberOfExtractedLinesOfCode;
		this.numberOfParametersInExtractedMethod = metrics.numberOfParametersInExtractedMethod;
		this.changes = new ArrayList<>(changes);
		this.undoChanges = new ArrayList<>(undoChanges);
		this.reductionOfCognitiveComplexity = metrics.reductionOfCognitiveComplexity;
		this.accumulatedInherentComponent = metrics.accumulatedInherentComponent;
		this.accumulatedNestingComponent = metrics.accumulatedNestingComponent;
		this.numberNestingContributors = metrics.numberNestingContributors;
		this.nesting = metrics.nesting;
	}
	
	public void joinMetrics(CodeExtractionMetrics metrics) {
		this.feasible = (this.feasible && metrics.isFeasible());
		this.reason += "\n " + metrics.getReason();
		this.applied = (this.applied && metrics.isApplied());
		this.numberOfExtractedLinesOfCode += metrics.getNumberOfExtractedLinesOfCode();
		this.numberOfParametersInExtractedMethod += metrics.getNumberOfParametersInExtractedMethod();
		this.changes.addAll(metrics.getChanges());
		this.undoChanges.addAll(metrics.getUndoChanges());
		this.reductionOfCognitiveComplexity += metrics.reductionOfCognitiveComplexity;
		this.accumulatedInherentComponent = 0;
		this.accumulatedNestingComponent = 0;
		this.numberNestingContributors = 0;
		this.nesting = 0;
	}

	public boolean isFeasible() {
		return feasible;
	}

	public void setFeasible(boolean feasible) {
		this.feasible = feasible;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean isApplied() {
		return applied;
	}

	public void setApplied(boolean applied) {
		this.applied = applied;
	}

	public int getNumberOfExtractedLinesOfCode() {
		return numberOfExtractedLinesOfCode;
	}

	public void setNumberOfExtractedLinesOfCode(int numberOfExtractedLinesOfCode) {
		this.numberOfExtractedLinesOfCode = numberOfExtractedLinesOfCode;
	}

	public int getReductionOfCognitiveComplexity() {
		return reductionOfCognitiveComplexity;
	}

	public void setReductionOfCognitiveComplexity(int reductionOfCognitiveComplexity) {
		this.reductionOfCognitiveComplexity = reductionOfCognitiveComplexity;
	}

	public int getCognitiveComplexityOfNewExtractedMethod() {
		return accumulatedInherentComponent + accumulatedNestingComponent;
	}

	public int getNumberOfParametersInExtractedMethod() {
		return this.numberOfParametersInExtractedMethod;
	}

	public void setNumberOfParametersInExtractedMethod(int numberOfParametersInExtractedMethod) {
		this.numberOfParametersInExtractedMethod = numberOfParametersInExtractedMethod;
	}

	public int getAccumulatedInherentComponent() {
		return this.accumulatedInherentComponent;
	}

	public void setAccumulatedInherentComponent(int accumulatedInherentComponent) {
		this.accumulatedInherentComponent = accumulatedInherentComponent;
	}

	public int getAccumulatedNestingComponent() {
		return this.accumulatedNestingComponent;
	}

	public void setAccumulatedNestingComponent(int accumulatedNestingComponent) {
		this.accumulatedNestingComponent = accumulatedNestingComponent;
	}

	public int getNumberNestingContributors() {
		return this.numberNestingContributors;
	}

	public void setNumberNestingContributors(int numberNestingContributors) {
		this.numberNestingContributors = numberNestingContributors;
	}

	public List<Change> getChanges() {
		return changes;
	}

	public void setChanges(List<Change> changes) {
		this.changes = changes;
	}

	public List<Change> getUndoChanges() {
		return undoChanges;
	}

	public void setUndoChanges(List<Change> undoChanges) {
		this.undoChanges = undoChanges;
	}

	@Override
	public String toString() {
		String result;

		result = "feasible = " + feasible + ", " + "applied = " + applied + ", " + "reason = " + reason + ", "
				+ "numberOfExtractedLinesOfCode = " + numberOfExtractedLinesOfCode + ", "
				+ "numberOfParametersInExtractedMethod = " + numberOfParametersInExtractedMethod + ", " + "changes = "
				+ changes.size() + ", " + "undoChanges = " + undoChanges.size();

		return result;
	}

	public int getNesting() {
		return nesting;
	}

	public void setNesting(int nesting) {
		this.nesting = nesting;
	}

}
