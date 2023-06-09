package neo.reducecognitivecomplexity.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;
import neo.reducecognitivecomplexity.jdt.Utils;
import neo.reducecognitivecomplexity.refactoringcache.RefactoringCache;

/**
 * A sequence is a list of sibling nodes of the AST (list of statements at the
 * same level in the original {@link CompilationUnit}).
 */
public class Sequence {
	/**
	 * Compilation unit this Sequence belongs to
	 */
	private CompilationUnit compilationUnit;

	/**
	 * {@link siblingNodes} list of sibling nodes.
	 */
	private List<ASTNode> siblingNodes;

	/**
	 * Create an empty Sequence for a given compilation unit
	 * 
	 * @param Compilation unit sibling nodes belongs to
	 */
	public Sequence(CompilationUnit compilationUnit) {
		this.siblingNodes = new ArrayList<ASTNode>();
		this.compilationUnit = compilationUnit;
	}

	/**
	 * Create a Sequence for the given compilation unit from a given list of sibling nodes
	 * 
	 * @param Compilation unit sibling nodes belongs to
	 * @param sibling nodes
	 */
	public Sequence(CompilationUnit compilationUnit, List<ASTNode> siblingNodes) {
		this.compilationUnit = compilationUnit;
		this.siblingNodes = siblingNodes;
	}

	/**
	 * Compute metrics of the code extraction associated to the Sequence.
	 * 
	 * This is done by asking Eclipse to perform the refactoring, compile new source
	 * code, collect metrics, and undo the previous refactoring operation.
	 * 
	 * Calls to this method are slower than calling {@link evaluate(RefactoringCache
	 * rf)}.
	 * 
	 * @return Metrics of the code extraction associated to the Sequence.
	 */
	public CodeExtractionMetrics evaluate() {
		CodeExtractionMetrics result;

		ASTNode nodeA = this.siblingNodes.get(0);
		while (nodeA != null && !(nodeA instanceof Statement)) {
			nodeA = nodeA.getParent();
		}
		ASTNode nodeB = this.siblingNodes.get(this.siblingNodes.size() - 1);
		while (nodeB != null && !(nodeB instanceof Statement)) {
			nodeB = nodeB.getParent();
		}
		
		result = neo.reducecognitivecomplexity.jdt.Utils.checkCodeExtractionBetweenTwoNodes(compilationUnit, nodeA,
				nodeB);

		return result;
	}

	/**
	 * Compute metrics of the code extraction associated to the Sequence using the
	 * provided {@link RefactoringCache}. Calls to this method are faster than
	 * calling {@link evaluate(CompilationUnit compilationUnit)}.
	 * 
	 * @param rf
	 * @return Metrics of the code extraction associated to the Sequence.
	 */
	public CodeExtractionMetrics evaluate(RefactoringCache rf) {
		CodeExtractionMetrics result;
		result = rf.getMetrics(this);
		return result;
	}

	/**
	 * Get nesting component of cognitive complexity of the Sequence.
	 * 
	 * @return -1 if sibling list is empty
	 */
	public int getNesting() {
		if (siblingNodes.size() == 0) {
			return -1;
		}
		return Utils.computeNesting(siblingNodes.get(0));
	}

	public List<ASTNode> getSiblingNodes() {
		return siblingNodes;
	}

	public void setSiblingNodes(List<ASTNode> siblingNodes) {
		this.siblingNodes = siblingNodes;
	}

	@Override
	public String toString() {
		String result = "[";

		for (int i = 0; i < siblingNodes.size() - 1; i++) {
			result += siblingNodes.get(i).getStartPosition() + " ";
		}
		result += siblingNodes.get(siblingNodes.size() - 1).getStartPosition() + "]";

		return result;
	}

	/**
	 * Get the accumulated cognitive complexity of the Sequence
	 * 
	 * @return Accumulated cognitive complexity of the Sequence
	 */
	public int getAccumulatedCognitiveComplexity() {
		return siblingNodes.stream()
				.mapToInt(node -> Utils.getIntegerPropertyOfNode(node, Constants.ACCUMULATED_COMPLEXITY)).sum();
	}

	/**
	 * Get the cognitive complexity of the Sequence
	 * 
	 * @return Accumulated cognitive complexity of the Sequence
	 */
	public int getComplexityWhenExtracted() {
		return getAccumulatedInherentComponent() + getAccumulatedNestingComponent();
	}

	/**
	 * Get the accumulated inherent component of cognitive complexity of the
	 * Sequence
	 * 
	 * @return Accumulated inherent component of cognitive complexity of the
	 *         Sequence
	 */
	public int getAccumulatedInherentComponent() {
		return siblingNodes.stream().mapToInt(
				node -> Utils.getIntegerPropertyOfNode(node, Constants.ACCUMULATED_INHERENT_COMPLEXITY_COMPONENT))
				.sum();
	}

	/**
	 * Get the accumulated nesting component of cognitive complexity of the Sequence
	 * 
	 * @return Accumulated nesting component of cognitive complexity of the Sequence
	 */
	public int getAccumulatedNestingComponent() {
		return siblingNodes.stream().mapToInt(
				node -> Utils.getIntegerPropertyOfNode(node, Constants.ACCUMULATED_NESTING_COMPLEXITY_COMPONENT)).sum();
	}

	/**
	 * Get the number of nesting contributors of the Sequence
	 * 
	 * @return Number of nesting contributors of the Sequence
	 */
	public int getNumberNestingContributors() {
		return siblingNodes.stream().mapToInt(node -> Utils.getIntegerPropertyOfNode(node,
				Constants.ACCUMULATED_NUMBER_NESTING_COMPLEXITY_CONTRIBUTORS)).sum();
	}

	/**
	 * Check if the Sequence contains the given node
	 * 
	 * @param node
	 * @return True if the Sequence contains the given node
	 */
	public boolean contains(ASTNode node) {
		return siblingNodes.contains(node);
	}

	/**
	 * Copy the current Sequence
	 * 
	 * @return A copy of the current Sequence
	 */
	public Sequence copy() {
		Sequence result = new Sequence(this.compilationUnit);
		result.siblingNodes.addAll(this.siblingNodes);
		return result;
	}

	public String toString2() {
		String result = "[";

		for (int i = 0; i < siblingNodes.size() - 1; i++) {
			result += siblingNodes.get(i) + ",";
		}
		result += siblingNodes.get(siblingNodes.size() - 1) + "]";

		return result;
	}
	
	public String getOffset() {
		String result = "[";
		int initialOffset, endOffset;
		ASTNode lastNode;
		
		if (siblingNodes.size() > 0) {
			initialOffset = siblingNodes.get(0).getStartPosition();
			lastNode = siblingNodes.get(siblingNodes.size()-1);
			int originalStartPositionOfLastSequence = lastNode.getStartPosition();
			while (lastNode != null && !(lastNode instanceof Statement) )
				lastNode = lastNode.getParent();
			int newStartPositionOfLastSequence = lastNode.getStartPosition();
			if (originalStartPositionOfLastSequence != newStartPositionOfLastSequence)
			{
				System.err.println("ERROR when proccessing sequence " + this.toString());
				System.err.println("newStartPositionOfLastSequence = " + newStartPositionOfLastSequence);
				System.err.println("originalStartPositionOfLastSequence = " + originalStartPositionOfLastSequence);
		
				//System.exit(-1);
			}
			endOffset = (lastNode.getStartPosition() + lastNode.getLength());
			
			result = result + initialOffset + ", " + endOffset;
		}
		
		result += "]";

		return result;
	}
	
	/**
	 * Get the {@link Pair} associated to the sequence
	 * @return the {@link Pair} associated to the sequence
	 */
	public Pair getOffsetAsPair() {
		String offset = getOffset();
		Pair result = null;
		
		result = new Pair(offset);

		return result;
	}

	/**
	 * Extract the Sequence as a new method in the same compilation unit.
	 * 
	 * @param compilationUnit     The compilation unit under processing.
	 * @param extractedMethodName Name of the new extracted method.
	 * @return Metrics of the code extraction: if the extraction was feasible,
	 *         applied, the reason why the extraction failed, the length of the
	 *         extracted code, ...
	 */
	public CodeExtractionMetrics extractSequence(CompilationUnit compilationUnit, String extractedMethodName) {
		CodeExtractionMetrics result;
		ASTNode nodeA = this.siblingNodes.get(0);
		while (nodeA != null && !(nodeA instanceof Statement)) {
			nodeA = nodeA.getParent();
		}
		ASTNode nodeB = this.siblingNodes.get(this.siblingNodes.size() - 1);
		while (nodeB != null && !(nodeB instanceof Statement)) {
			nodeB = nodeB.getParent();
		}
		
		result = neo.reducecognitivecomplexity.jdt.Utils.extractCodeBetweenTwoNodes(compilationUnit, nodeA, nodeB,
				extractedMethodName, false);

		return result;
	}
	
	/**
	 * Get the method declaration associated to the Sequence
	 * @return Node associated to the method declaration of this Sequence
	 */
	public MethodDeclaration getMethodDeclaration() {
		ASTNode result=null;
		
		if (!this.siblingNodes.isEmpty()) {
			result = siblingNodes.get(0);
			
			while (!(result instanceof MethodDeclaration)) {
				result = result.getParent();
				
				// A method could declare more methods in an anonymous class. Skip those cases
				if (result instanceof MethodDeclaration)
				{
					if (result.getParent() instanceof AnonymousClassDeclaration) {
						result = result.getParent();
					}
				}
			}
		}
		
		return (MethodDeclaration) result;
	}
}
