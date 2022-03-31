package neo.reducecognitivecomplexity.algorithms.exhaustivesearch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.algorithms.RefactoringCache;
import neo.reducecognitivecomplexity.algorithms.Solution;
import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.ConsecutiveSequenceIterator.APPROACH;

/**
 * Enumerative search for finding solutions ({@link Solution}) to reduce methods
 * cognitive complexity
 */
public class EnumerativeSearch {
	private Solution bestSolution;

	public Solution run(APPROACH approach, BufferedWriter bf, String classWithIssues, CompilationUnit compilationUnit,
			RefactoringCache refactoringCache, List<ASTNode> auxList, ASTNode ast, int methodComplexity)
			throws IOException {
		bestSolution = null;
		int optimo = 0;

		long startTime = System.currentTimeMillis();
		ExhaustiveEnumerationAlgorithm eea = new ExhaustiveEnumerationAlgorithm(refactoringCache, ast, approach);
		try {
			eea.run(solution -> {
				Solution sol = new Solution(solution, compilationUnit, ast);
				sol.evaluate(refactoringCache);
				if (bestSolution == null || sol.getFitness() < bestSolution.getFitness()) {
					bestSolution = sol;
				}
			}, Constants.MAX_EVALS);
		} catch (RuntimeException e) {
			System.out.print("Optimal " + bestSolution.toString());
			optimo = 1;
		}

		long runtime = System.currentTimeMillis() - startTime;

		// write ES results into file
		bf.append(approach.name() + ";");
		bf.append(classWithIssues + ";");
		bf.append(((MethodDeclaration) ast).getName().toString() + ";");
		bf.append(methodComplexity + ";");
		bf.append(bestSolution.toStringForFileFormat() + ";");
		bf.append(bestSolution.getSize() + ";");
		bf.append(bestSolution.getFitness() + ";");
		bf.append(bestSolution.getReducedComplexity() + ";");
		bf.append(methodComplexity - bestSolution.getReducedComplexity() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMinNumberOfExtractedLinesOfCode() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMaxNumberOfExtractedLinesOfCode() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMeanNumberOfExtractedLinesOfCode() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getTotalNumberOfExtractedLinesOfCode() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMinNumberOfParametersInExtractedMethods() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMaxNumberOfParametersInExtractedMethods() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMeanNumberOfParametersInExtractedMethods() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getTotalNumberOfParametersInExtractedMethods() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMinReductionOfCognitiveComplexity() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMaxReductionOfCognitiveComplexity() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getMeanReductionOfCognitiveComplexity() + ";");
		bf.append(bestSolution.getExtractionMetricsStats().getTotalNumberOfReductionOfCognitiveComplexity() + ";");
		bf.append("" + optimo + ";");
		bf.append(Long.toString(runtime) + "\n");

		bf.flush();

		return bestSolution;
	}

}