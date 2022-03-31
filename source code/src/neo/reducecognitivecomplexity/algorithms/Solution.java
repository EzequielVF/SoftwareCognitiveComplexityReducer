package neo.reducecognitivecomplexity.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.ltk.core.refactoring.Change;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetricsStats;
import neo.reducecognitivecomplexity.jdt.Utils;

/**
 * A Solution is a list of code extractions to reduce the cognitive complexity
 * of a method. Each code extraction is represented by a {@link Sequence}.
 */
public class Solution {
	/**
	 * list of {@link Sequence}. The list is sorted by the order of nodes in the
	 * corresponding compilation unit (offset in the source code). The sorting is
	 * made when inserting elements in the list.
	 */
	private List<Sequence> sequenceList;

	/**
	 * the {@link CompilationUnit} this Solution belongs to.
	 */
	private CompilationUnit compilationUnit;

	/**
	 * the AST of the {@link CompilationUnit} this Solution belongs to. This usually
	 * points to the {@link MethodDeclaration} node of the {@link CompilationUnit}
	 * under processing.
	 */
	private ASTNode method;

	/**
	 * name of the method this Solution belongs to.
	 */
	private String methodName;

	/**
	 * True when the code extractions can be applied.
	 */
	private boolean feasible = false;

	/**
	 * Quality of the Solution (the greater the value the worst)
	 */
	private double fitness;

	/**
	 * Cognitive complexity reduction if the Solution is applied
	 */
	private int reducedComplexity;

	/**
	 * Metrics of the code extraction associated to this Solution
	 */
	private CodeExtractionMetricsStats extractionMetricsStats = null;

	/**
	 * Create a Solution from a given {@link Sequence} list
	 * 
	 * @param sequenceList          The {@link Sequence} list
	 * @param compilationUnit       {@link CompilationUnit} associated to the
	 *                              {@link Sequence} list
	 * @param methodDeclarationNode {@link MethodDeclaration} node
	 */
	public Solution(List<Sequence> sequenceList, CompilationUnit compilationUnit, ASTNode methodDeclarationNode) {
		this.sequenceList = sequenceList;
		this.compilationUnit = compilationUnit;
		this.method = methodDeclarationNode;
		this.methodName = ((MethodDeclaration) this.method).getName().toString();
	}

	/**
	 * Create an empty Solution for a given {@link CompilationUnit}
	 * 
	 * @param compilationUnit       {@link CompilationUnit} to be associated to the
	 *                              Solution
	 * @param methodDeclarationNode {@link MethodDeclaration} node
	 */
	public Solution(CompilationUnit compilationUnit, ASTNode methodDeclarationNode) {
		this.sequenceList = new ArrayList<>();
		this.compilationUnit = compilationUnit;
		this.method = methodDeclarationNode;
		this.methodName = ((MethodDeclaration) this.method).getName().toString();
	}

	/**
	 * Create a Solution from another Solution
	 * 
	 * @param currentSolution
	 */
	public Solution(Solution currentSolution) {
		// It should be a copy of the currentSolution
		this.compilationUnit = currentSolution.compilationUnit;
		this.method = currentSolution.method;
		this.methodName = ((MethodDeclaration) currentSolution.method).getName().toString();
		this.sequenceList = new ArrayList<>();
		for (Sequence s : currentSolution.sequenceList) {
			this.sequenceList.add(s.copy());
		}
		this.fitness = currentSolution.fitness;
		this.feasible = currentSolution.feasible;
		this.reducedComplexity = currentSolution.reducedComplexity;
		this.extractionMetricsStats = currentSolution.getExtractionMetricsStats();
	}

	/**
	 * Compute the fitness of the solution using the provided
	 * {@link RefactoringCache} and returns Solution's metrics.
	 * 
	 * @param rf
	 * @return Metrics of the solution.
	 */
	public CodeExtractionMetrics evaluate(RefactoringCache rf) {
		CodeExtractionMetrics[] metrics = new CodeExtractionMetrics[sequenceList.size()];
		fitness = sequenceList.size();
		CodeExtractionMetrics results = new CodeExtractionMetrics(true, "", false, 0, 0, new ArrayList<Change>(),
				new ArrayList<Change>());

		// The list of sequences is processed from right to left
		for (int i = sequenceList.size() - 1; i >= 0; i--) {
			// Evaluate the fitness of the current sequence
			Sequence seq = sequenceList.get(i);
			metrics[i] = seq.evaluate(rf);

			// If the sequence is not feasible, return MAX_FITNESS
			if (!metrics[i].isFeasible()) {
				fitness = Double.MAX_VALUE;
				feasible = false;
				reducedComplexity = 0;
				return metrics[i];
			}
			// The sequence might exceed the complexity
			// For all nodes in the sequence
			int complexityOut = complexityOfSubtreeAfterExtraction(seq);
			// We penalize if the sequence exceed complexity
			if (complexityOut > Constants.MAX_COMPLEXITY) {
				fitness += (complexityOut - Constants.MAX_COMPLEXITY) * 10;
			}

			// Accumulate solution metrics
			results.joinMetrics(metrics[i]);
		}

		this.extractionMetricsStats = new CodeExtractionMetricsStats(metrics);

		int complexity = complexityOfSubtreeAfterExtraction(method);
		reducedComplexity = Utils.getIntegerPropertyOfNode(method, Constants.ACCUMULATED_COMPLEXITY) - complexity;

		// We penalize when the main method still have more than MAX_COMPLEXITY
		if (complexity > Constants.MAX_COMPLEXITY) {
			fitness += (complexity - Constants.MAX_COMPLEXITY) * 10;
		}
		feasible = results.isFeasible();

		return results;
	}

	/**
	 * Check if the list of sequence of nodes can be extracted.
	 * 
	 * @return true if the list of sequence of nodes can be extracted.
	 */
	public boolean isFeasible() {
		return feasible;
	}

	/**
	 * Insert a sequence of nodes in the list of sequences. The insertion is made
	 * taking into account the place of the {@link Sequence} in the
	 * {@link CompilationUnit}. Thus, the insertion operation keeps the list sorted.
	 * 
	 * @param s sequence of nodes to insert in the list of sequences.
	 */
	public void insertSequence(Sequence s) {
		int positionOfInsertion = 0;
		boolean sequenceInserted = false;
		int startPositionOfSequenceToInsert = s.getSiblingNodes().get(0).getStartPosition();

		while (!this.sequenceList.isEmpty() && !sequenceInserted && positionOfInsertion < this.sequenceList.size()) {
			int startPositionOfCurrentSequence = this.sequenceList.get(positionOfInsertion).getSiblingNodes().get(0)
					.getStartPosition();
			if (startPositionOfSequenceToInsert < startPositionOfCurrentSequence) {
				this.sequenceList.add(positionOfInsertion, s);
				sequenceInserted = true;
			}
			positionOfInsertion++;
		}
		if (!sequenceInserted) {
			this.sequenceList.add(positionOfInsertion, s);
		}
	}

	public void removeSequence(int i) {
		this.sequenceList.remove(i);

	}

	public List<Sequence> getSequenceList() {
		return this.sequenceList;

	}

	public Sequence getSequence(int i) {
		return this.sequenceList.get(i);

	}

	public int getSize() {
		return this.sequenceList.size();
	}

	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Create a random Solution from a given {@link ASTNode}.
	 * 
	 * @param nodes
	 * @param r
	 */
	public void randomSolution(List<ASTNode> nodes, Random r) {
		for (ASTNode node : nodes) {
			if (r.nextDouble() < 1) {
				Sequence s = new Sequence();
				s.getSiblingNodes().add(node);
				if (s.evaluate(compilationUnit).isFeasible()) {
					insertSequence(s);
				}
			}
		}

	}

	public boolean contains(ASTNode node) {
		return sequenceList.stream().anyMatch(s -> s.contains(node));
	}

	@Override
	public String toString() {
		return "Solution [methodName=" + methodName + ", sequenceList=" + sequenceList + ", isFeasible=" + feasible
				+ ", fitness=" + fitness + ", reducedComplexity=" + reducedComplexity + "]";
	}

	public String toStringForFileFormat() {
		return this.sequenceList.toString();
	}

	public double getFitness() {
		return fitness;
	}

	public int getReducedComplexity() {
		return reducedComplexity;
	}

	public CodeExtractionMetricsStats getExtractionMetricsStats() {
		return extractionMetricsStats;
	}

	private int complexityOfSubtreeAfterExtraction(ASTNode root) {
		final int nodeNesting = Utils.computeNesting(root);
		CCSubtreeComputer ccSubtreeComputer = new CCSubtreeComputer(nodeNesting, root);
		root.accept(ccSubtreeComputer);
		return ccSubtreeComputer.getComplexity();
	}

	private int complexityOfSubtreeAfterExtraction(Sequence sequence) {
		return sequence.getSiblingNodes().stream().mapToInt(this::complexityOfSubtreeAfterExtraction).sum();
	}

	private final class CCSubtreeComputer extends ASTVisitor {
		private final int nodeNesting;
		private final ASTNode root;
		private int complexity = 0;

		private CCSubtreeComputer(int nodeNesting, ASTNode root) {
			this.nodeNesting = nodeNesting;
			this.root = root;
		}

		@Override
		public String toString() {
			return "CCSubtreeComputer [nodeNesting=" + nodeNesting + ", root=" + root + ", complexity=" + complexity
					+ "]";
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (node != root && Solution.this.contains(node)) {
				return false;
			}

			int totalCC = Utils.getIntegerPropertyOfNode(node, Constants.CONTRIBUTION_TO_COMPLEXITY);
			if (totalCC != 0) {
				if (totalCC > nodeNesting) {
					complexity += totalCC - nodeNesting;
				} else {
					complexity += totalCC;
				}
			}
			return super.preVisit2(node);
		}

		public int getComplexity() {
			return complexity;
		}
	}
}
