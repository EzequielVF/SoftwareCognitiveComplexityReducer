package neo.reducecognitivecomplexity.refactoringcache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.Change;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;

import neo.reducecognitivecomplexity.Utils;
import neo.reducecognitivecomplexity.algorithms.Pair;
import neo.reducecognitivecomplexity.algorithms.Sequence;
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
	 * Initialize the refactoring cache from a CSV file
	 * It assumes that the CSV file has the following header and format:
	 * "Offset_A, Offset_B, feasibility, reason, #parameters, extractedLOC, reductionCC, extractedMethodCC, accumulatedInherentComponent, accumulatedNestingComponent, numberNestingContributors, nesting"
	 * @param path     of the CSV file
	 * @param fileName of the CSV file
	 * @throws IOException
	 */
	public  RefactoringCache(String path, String fileName, CompilationUnit compilationUnit) throws IOException {
		this.cache = new HashMap<>();
		this.compilationUnit = compilationUnit;
		
		BufferedReader br = new BufferedReader(new FileReader(path + fileName));
		String line = br.readLine(); //read CSV header
		line = br.readLine();
		while (line != null)
		{
			String[] tokens = line.split(",");
			CodeExtractionMetrics metrics = new CodeExtractionMetrics(Integer.valueOf(tokens[2].trim())==1, 
												tokens[3], 
												false, 
												Integer.valueOf(tokens[5].trim()), 
												Integer.valueOf(tokens[4].trim()), 
												new ArrayList<Change>(), 
												new ArrayList<Change>(),
												Integer.valueOf(tokens[6].trim()),
												Integer.valueOf(tokens[8].trim()),
												Integer.valueOf(tokens[9].trim()),
												Integer.valueOf(tokens[10].trim()),
												Integer.valueOf(tokens[11].trim())
												);
			Pair pair = new Pair(Integer.valueOf(tokens[0].trim()), Integer.valueOf(tokens[1].trim()));
			this.cache.put(pair, metrics);
			line = br.readLine();
		}
		br.close();
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

		Pair key = sequence.getOffsetAsPair();
	
		CodeExtractionMetrics result = cache.get(key);
		if (result == null) {
			result = sequence.evaluate();

			result.setReductionOfCognitiveComplexity(sequence.getAccumulatedCognitiveComplexity());
			result.setAccumulatedInherentComponent(sequence.getAccumulatedInherentComponent());
			result.setAccumulatedNestingComponent(sequence.getAccumulatedNestingComponent());
			result.setNumberNestingContributors(sequence.getNumberNestingContributors());
			result.setNesting(sequence.getNesting());

			cache.put(key, result);
		}
		
		// A copy (not reference) of the metrics 
		return new CodeExtractionMetrics(result);
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

		content += "A, B, feasible, reason, parameters, extractedLOC, reductionCC, extractedMethodCC, accumulatedInherentComponent, accumulatedNestingComponent, numberNestingContributors, nesting";

		for (Entry<Pair, CodeExtractionMetrics> entry : cache.entrySet()) {
			String reason = entry.getValue().getReason();
			
			if (!entry.getValue().isFeasible())
				reason = reason.replaceAll(System.lineSeparator(), " ");
			
			content += System.lineSeparator();
			content += entry.getKey().getA() + ", " + entry.getKey().getB() + ", "
					+ (entry.getValue().isFeasible() ? "1" : "0") + ", " + "\"" + reason + "\", "
					+ entry.getValue().getNumberOfParametersInExtractedMethod() + ", "
					+ entry.getValue().getNumberOfExtractedLinesOfCode() + ", "
					+ entry.getValue().getReductionOfCognitiveComplexity() + ", "
					+ entry.getValue().getCognitiveComplexityOfNewExtractedMethod() + ", "
					+ entry.getValue().getAccumulatedInherentComponent() + ", "
					+ entry.getValue().getAccumulatedNestingComponent() + ", "
					+ entry.getValue().getNumberNestingContributors() + ", "
					+ entry.getValue().getNesting();
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
			ExtractionVertex root,
			int methodCognitiveComplexity,
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
			if (result.addVertex(root)){
				for (ExtractionVertex v : result.vertexSet()) {
					if (!v.equals(root)) {
						if (result.outDegreeOf(v) == 0) {
							edge = result.addEdge(v, root);
							result.setEdgeWeight(edge, 1);
						}
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

	/**
	 * Generate reduced directed weight graphs associated to the refactoring cache
	 *
	 * @param methodCognitiveComplexity
	 * @param graphWithoutConflicts     target reduced graph to store no conflicts
	 * @param conflictsGraph            target reduced graph to store conflicts
	 * @return Reduced directed weight graph including also conflicts
	 */
	public SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> reduceGraphOfFeasibleRefactorings(
			ExtractionVertex vertexForMethod, int methodCognitiveComplexity,
			SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> graphWithoutConflicts,
			SimpleGraph<ExtractionVertex, DefaultEdge> conflictsGraph) {
		SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> result = new SimpleDirectedWeightedGraph<>(
				DefaultWeightedEdge.class);
		SimpleGraph<ExtractionVertex, DefaultEdge> conflictsReducedGraph = new SimpleGraph<>(DefaultEdge.class);

		// store the current graph that does not contain conflicts
		neo.reducecognitivecomplexity.graphs.Utils.copy(graphWithoutConflicts, result);

		// store the current conflicts graph
		neo.reducecognitivecomplexity.graphs.Utils.copy(conflictsGraph, conflictsReducedGraph);

		for (ExtractionVertex currentVertex : graphWithoutConflicts.vertexSet()) {
			if (!currentVertex.equals(vertexForMethod)) {
				List<ExtractionVertex> children = neo.reducecognitivecomplexity.graphs.Utils
						.immediatePreviousVertices(result, currentVertex);
				for (ExtractionVertex child : children) {
					// nodes have the same cognitive complexity (they can be reduced)
					if (currentVertex.getReductionOfCognitiveComplexity() == child
							.getReductionOfCognitiveComplexity()) {
						// remove currentVertex
						neo.reducecognitivecomplexity.graphs.Utils.remove(result, currentVertex);
						conflictsReducedGraph.removeVertex(currentVertex);
						break;
					}

				}
			}
		}

		// transitivity reduction on the graph
		TransitiveReduction.INSTANCE.reduce(result);

		// clean original graphs
		neo.reducecognitivecomplexity.graphs.Utils.clear(graphWithoutConflicts);
		neo.reducecognitivecomplexity.graphs.Utils.clear(conflictsGraph);

		// store reduced graphs
		neo.reducecognitivecomplexity.graphs.Utils.copy(result, graphWithoutConflicts);
		neo.reducecognitivecomplexity.graphs.Utils.copy(conflictsReducedGraph, conflictsGraph);

		// add conflict edges to the graph
		DefaultWeightedEdge we;
		for (DefaultEdge e : conflictsReducedGraph.edgeSet()) {
			we = result.addEdge(conflictsReducedGraph.getEdgeSource(e), conflictsReducedGraph.getEdgeTarget(e));
			result.setEdgeWeight(we, 0);

			we = result.addEdge(conflictsReducedGraph.getEdgeTarget(e), conflictsReducedGraph.getEdgeSource(e));
			result.setEdgeWeight(we, 0);
		}

		return result;
	}

	/**
	 * Reduce the refactoring cache removing overlapped extractions
	 * 
	 * @return The reduced refactoring cache
	 */
	public RefactoringCache reduce() {
		Map<Pair, CodeExtractionMetrics> feasibleRefactorings;
		List<Pair> offsetPairs;
		RefactoringCache result = new RefactoringCache(compilationUnit);

		feasibleRefactorings = Utils.filterByValue(cache, value -> value.isFeasible());
		offsetPairs = new ArrayList<Pair>(feasibleRefactorings.keySet());

		// Iterate the list of offset pairs
		for (int i = 0; i < offsetPairs.size(); i++) {
			Pair p = offsetPairs.get(i);
			CodeExtractionMetrics codeExtractionMetricsP = cache.get(p);
			result.cache.put(p, codeExtractionMetricsP);

			// Iterate over all elements of the list
			for (int j = 0; j < offsetPairs.size(); j++) {
				if (i != j) {
					Pair q = offsetPairs.get(j);
					CodeExtractionMetrics codeExtractionMetricsQ = cache.get(q);

					if (codeExtractionMetricsP.getReductionOfCognitiveComplexity() == codeExtractionMetricsQ
							.getReductionOfCognitiveComplexity()) {
						// q is contained in p
						if (Pair.isContained(q, p)) {
							result.cache.remove(p);
						}
						// p is contained in q
						else if (Pair.isContained(p, q)) {
							result.cache.remove(q);
						}
					}
				}
			}
		}

		return result;
	}

	public CompilationUnit getCompilationUnit() {
		return this.compilationUnit;
	}
}
