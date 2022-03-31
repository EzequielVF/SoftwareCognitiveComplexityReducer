package neo.reducecognitivecomplexity.algorithms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;

import neo.reducecognitivecomplexity.Utils;
import neo.reducecognitivecomplexity.graphs.ExtractionVertex;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;

/**
 * Model a cache of refactoring opportunities found during the search.
 * 
 * This is useful to speed up the search of sequences of code extractions. When
 * a code extraction ({@link Sequence}) is evaluated, the refactoring cache is
 * queried. If there is a hit, metrics of this code extraction are obtained from
 * the cache. If not, the oracle is called to evaluate the feasibility of this
 * code extraction and the cache is updated with this information.
 */
public class RefactoringCache {
	private CompilationUnit compilationUnit;
	public Map<Pair, CodeExtractionMetrics> cache;

	public RefactoringCache(CompilationUnit compilationUnit) {
		cache = new HashMap<>();
		this.compilationUnit = compilationUnit;
	}

	/**
	 * Get metrics of
	 * 
	 * @param sequence
	 * @return Metrics if the current sequence is extracted or null is the sequence
	 *         is empty
	 */
	public CodeExtractionMetrics getMetrics(Sequence sequence) {
		if (sequence.getSiblingNodes().isEmpty()) {
			return null;
		}
		ASTNode firstNode = sequence.getSiblingNodes().get(0);
		ASTNode lastNode = sequence.getSiblingNodes().get(sequence.getSiblingNodes().size() - 1);
		int from = firstNode.getStartPosition();
		int to = lastNode.getStartPosition() + lastNode.getLength() - 1;
		Pair key = new Pair(from, to);

		CodeExtractionMetrics result = cache.get(key);
		if (result == null) {
			result = sequence.evaluate(compilationUnit);

			result.setReductionOfCognitiveComplexity(sequence.getAccumulatedCognitiveComplexity());
			result.setAccumulatedInherentComponent(sequence.getAccumulatedInherentComponent());
			result.setAccumulatedNestingComponent(sequence.getAccumulatedNestingComponent());
			result.setNumberNestingContributors(sequence.getNumberNestingContributors());
			result.setNesting(sequence.getNesting());

			cache.put(key, result);
		}
		return result;
	}

	@Override
	public String toString() {
		String result = new String();
		int countFeasibleRefactorings = 0, countUnfeasibleRefactorings = 0;

		for (Entry<Pair, CodeExtractionMetrics> entry : cache.entrySet()) {
			if (entry.getValue().isFeasible())
				countFeasibleRefactorings++;
			else
				countUnfeasibleRefactorings++;
		}

		result += "Elements in the cache = " + cache.size() + " " + "(feasible: " + countFeasibleRefactorings + ", "
				+ "unfeasible: " + countUnfeasibleRefactorings + ")" + System.lineSeparator();

		return result;
	}

	/**
	 * Write the refactoring cache to a CSV file
	 * 
	 * @param path     of the CSV file
	 * @param fileName of the CSV file
	 * @throws IOException
	 */
	public void writeToCSV(String path, String fileName) throws IOException {
		String content = new String();
		BufferedWriter refactoringCacheInfo = new BufferedWriter(new FileWriter(path + fileName, false));

		content += "A, B, feasible, reason, parameters, extractedLOC, reductionCC, extractedMethodCC";

		for (Entry<Pair, CodeExtractionMetrics> entry : cache.entrySet()) {
			content += System.lineSeparator();
			content += entry.getKey().getA() + ", " + entry.getKey().getB() + ", "
					+ (entry.getValue().isFeasible() ? "1" : "0") + ", " + "\"" + entry.getValue().getReason() + "\", "
					+ entry.getValue().getNumberOfParametersInExtractedMethod() + ", "
					+ entry.getValue().getNumberOfExtractedLinesOfCode() + ", "
					+ entry.getValue().getReductionOfCognitiveComplexity() + ", "
					+ entry.getValue().getCognitiveComplexityOfNewExtractedMethod();
		}

		refactoringCacheInfo.append(content);
		refactoringCacheInfo.close();
	}

	/**
	 * Generate directed weight graphs associated to the refactoring cache
	 * 
	 * @param root                      node (usually the method declaration)
	 * @param methodCognitiveComplexity
	 * @param graphWithoutConflicts     target graph to store no conflicts
	 * @param conflictsGraph            target graph to store conflicts
	 * @return Directed weight graph including also conflicts
	 */
	public SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> getGraphOfFeasibleRefactorings(
			ExtractionVertex root, int methodCognitiveComplexity,
			SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> graphWithoutConflicts,
			SimpleGraph<ExtractionVertex, DefaultEdge> conflictsGraph) {
		Map<Pair, CodeExtractionMetrics> feasibleRefactorings;
		List<Pair> offsetPairs;
		DefaultWeightedEdge edge;
		SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> result;

		neo.reducecognitivecomplexity.graphs.Utils.clear(graphWithoutConflicts);
		neo.reducecognitivecomplexity.graphs.Utils.clear(conflictsGraph);

		feasibleRefactorings = Utils.filterByValue(cache, value -> value.isFeasible());
		offsetPairs = new ArrayList<Pair>(feasibleRefactorings.keySet());

		result = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

		// Iterate the list of offset pairs
		for (int i = 0; i < offsetPairs.size(); i++) {
			Pair p = offsetPairs.get(i);
			CodeExtractionMetrics codeExtractionMetrics = cache.get(p);
			ExtractionVertex vertexP = new ExtractionVertex(p.getA(), p.getB(),
					codeExtractionMetrics.getReductionOfCognitiveComplexity(),
					codeExtractionMetrics.getAccumulatedInherentComponent(),
					codeExtractionMetrics.getAccumulatedNestingComponent(),
					codeExtractionMetrics.getNumberNestingContributors(), codeExtractionMetrics.getNesting());
			result.addVertex(vertexP);

			// Iterate over the next elements of the list
			for (int j = i + 1; j < offsetPairs.size(); j++) {
				Pair q = offsetPairs.get(j);
				CodeExtractionMetrics codeExtractionMetrics2 = cache.get(q);
				ExtractionVertex vertexQ = new ExtractionVertex(q.getA(), q.getB(),
						codeExtractionMetrics2.getReductionOfCognitiveComplexity(),
						codeExtractionMetrics2.getAccumulatedInherentComponent(),
						codeExtractionMetrics2.getAccumulatedNestingComponent(),
						codeExtractionMetrics2.getNumberNestingContributors(), codeExtractionMetrics2.getNesting());
				result.addVertex(vertexQ);

				// q is contained in p
				if (Pair.isContained(q, p)) {
					edge = result.addEdge(vertexQ, vertexP);
					result.setEdgeWeight(edge, 1);
				}
				// p is contained in q
				else if (Pair.isContained(p, q)) {
					edge = result.addEdge(vertexP, vertexQ);
					result.setEdgeWeight(edge, 1);
				}
				// q overlapped by p (conflict)
				else if (Pair.overlapping(p, q)) {
					// add conflicts to conflict graph
					conflictsGraph.addVertex(vertexP);
					conflictsGraph.addVertex(vertexQ);
					conflictsGraph.addEdge(vertexQ, vertexP);
				}
			}
		}

		// add root and its corresponding edges to the graph
		if (root != null) {
			result.addVertex(root);
			for (ExtractionVertex v : result.vertexSet()) {
				if (!v.equals(root)) {
					if (result.outDegreeOf(v) == 0) {
						edge = result.addEdge(v, root);
						result.setEdgeWeight(edge, 1);
					}
				}
			}
		}

		// transitivity reduction on the graph
		TransitiveReduction.INSTANCE.reduce(result);

		// store the current graph that does not contain conflicts
		neo.reducecognitivecomplexity.graphs.Utils.copy(result, graphWithoutConflicts);

		// add conflict edges to the graph
		DefaultWeightedEdge we;
		for (DefaultEdge e : conflictsGraph.edgeSet()) {
			we = result.addEdge(conflictsGraph.getEdgeSource(e), conflictsGraph.getEdgeTarget(e));
			result.setEdgeWeight(we, 0);

			we = result.addEdge(conflictsGraph.getEdgeTarget(e), conflictsGraph.getEdgeSource(e));
			result.setEdgeWeight(we, 0);
		}

		return result;
	}
}
