package neo.reducecognitivecomplexity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.swt.widgets.Display;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;

import com.google.gson.Gson;

import neo.reducecognitivecomplexity.algorithms.RefactoringCache;
import neo.reducecognitivecomplexity.algorithms.Solution;
import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.EnumerativeSearch;
import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.RefactoringCacheFiller;
import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.ConsecutiveSequenceIterator.APPROACH;
import neo.reducecognitivecomplexity.graphs.ExtractionVertex;
import neo.reducecognitivecomplexity.jdt.Utils;
import neo.reducecognitivecomplexity.sonar.cognitivecomplexity.CognitiveComplexMethod;
import neo.reducecognitivecomplexity.sonar.cognitivecomplexity.ProjectIssues;

/**
 * This is the main procedure of the Eclipse plug-in. The application has 6
 * arguments: (1) the URL of SonarQube, (2) the name of the SONAR project which
 * has been previously analyzed, (3) the token to use the SONAR API REST, (4)
 * the name of the Eclipse project in the Eclipse workspace, (5) the name of the
 * Eclipse project in the Eclipse workspace for validation, and (6) algorithm to
 * run for the search of refactoring opportunities.
 */
public class Application implements IApplication {
	private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

	@Override
	public Object start(IApplicationContext arg) throws Exception {
		Display.getDefault(); // This is required to work in OSX systems: the display must be created in the
								// main thread

		String sonarServer, projectNameInSonar, projectNameInWorkspace, projectNameInWorkspaceForValidation, token, uri;
		String[] args = (String[]) arg.getArguments().get("application.args");

		// Check the number of arguments given
		if (args.length != Constants.ARGS) {
			LOGGER.severe("Number of arguments must be " + Constants.ARGS + "!");
			return -1;
		}

		// Read app arguments
		sonarServer = args[0];
		projectNameInSonar = args[1];
		token = args[2];
		projectNameInWorkspace = args[3];
		projectNameInWorkspaceForValidation = args[4];
		String algorithmName = args[5];

		try {
			// creating and adding information to the results file
			BufferedWriter bf = new BufferedWriter(
					new FileWriter(Constants.OUTPUT_FOLDER + algorithmName + "-" + Constants.FILE, false));
			bf.append("algorithm;class;method;initialComplexity;solution;extractions;fitness;"
					+ "reductionComplexity;finalComplexity;"
					+ "minExtractedLOC;maxExtractedLOC;meanExtractedLOC;totalExtractedLOC;"
					+ "minParamsExtractedMethods;maxParamsExtractedMethods;meanParamsExtractedMethods;totalParamsExtractedMethods;"
					+ "minReductionOfCC;maxReductionOfCC;meanReductionOfCC;totalReductionOfCC;" + "optimo;"
					+ "executionTime;" + "\n");

			// By default Sonar paginates queries to 100 elements per page. We have to
			// paginate the content.
			boolean allPagesProccessed = false;
			int currentPage = 1;
			while (!allPagesProccessed) {
				String json = null;

				// Compose SONAR server URI
				uri = neo.reducecognitivecomplexity.sonar.Utils.composeSonarUri(sonarServer, projectNameInSonar,
						currentPage);
				// Query cognitive complexity issues in project through the Sonar Web API
				json = neo.reducecognitivecomplexity.sonar.Utils.GETRequest(uri, token);

				// Parse json to get project issues
				Gson gson = new Gson();
				ProjectIssues issues = gson.fromJson(json, ProjectIssues.class);

				int totalPagesInSonar = (int) Math.ceil(issues.getTotal() / 100.0);
				LOGGER.info("Proccesing issues in page " + currentPage + " (over " + totalPagesInSonar + " pages)");

				// Read cognitive complex methods from issues reported by SONAR
				Map<String, List<CognitiveComplexMethod>> methodsWithIssues = ProjectIssues
						.getCognitiveComplexity(issues);

				// Iterate over classes containing cognitive complex methods
				LOGGER.info("#classes:" + methodsWithIssues.keySet().size());
				for (String classWithIssues : methodsWithIssues.keySet()) {
					LOGGER.info("Processing class '" + classWithIssues);

					// Get the relative path of current class in project in workspace
					String relativePathForFileToProcess = projectNameInWorkspace + File.separator + classWithIssues;

					List<Solution> solutions = new ArrayList<>();

					// Read compilation unit
					CompilationUnit compilationUnit = Utils
							.createCompilationUnitFromFileInWorkspace(relativePathForFileToProcess);

					// Validate if compilation unit is accessible and valid
					if (compilationUnit.getLength() == 0) {
						LOGGER.warning("ERROR WITH COMPILATION UNIT: " + compilationUnit.getTypeRoot().toString());
						LOGGER.warning(relativePathForFileToProcess);
					} else {
						// Iterate over cognitive complex methods in current class
						for (CognitiveComplexMethod complexMethod : methodsWithIssues.get(classWithIssues)) {
							String methodName;
							ExtractionVertex root;
							RefactoringCache refactoringCache = new RefactoringCache(compilationUnit);
							List<ASTNode> auxList = new ArrayList<ASTNode>();

							// Get AST of the method, including contribution to complexity reported by SONAR
							ASTNode ast = neo.reducecognitivecomplexity.sonar.Utils
									.getASTForMethodAnnotatingContributionToCognitiveComplexity(compilationUnit,
											complexMethod, auxList);

							// Get method name: this is the method name plus their signature
							// joined by dashes. We do this because could exist several methods with similar
							// names (but different signature)
							methodName = ((MethodDeclaration) ast).getName().toString();
							String composedMethodName = new String();
							if ((((MethodDeclaration) ast).parameters() != null)
									&& ((MethodDeclaration) ast).parameters().size() > 0) {
								composedMethodName = methodName + "-"
										+ String.join("-", Utils.getTypesInSignature((MethodDeclaration) ast));
							}

							LOGGER.info("Processing method '" + methodName + "' ...");
							LOGGER.info("Composed method name: '" + composedMethodName + "'");

							// We reduce composed method name length to avoid problems with the operative
							// system
							// replace special characters in composed method name
							composedMethodName = composedMethodName.replace('<', '-');
							composedMethodName = composedMethodName.replace('>', '-');
							composedMethodName = composedMethodName.replace('?', '-');
							composedMethodName = composedMethodName.replace(':', '-');
							composedMethodName = composedMethodName.replace('\\', '-');
							composedMethodName = composedMethodName.replace('/', '-');
							composedMethodName = composedMethodName.replace('*', '-');
							composedMethodName = composedMethodName.replace('|', '-');
							composedMethodName = composedMethodName.replace('"', '-');

							// define path for output files
							String fileNameForRefactoringCacheInfo = new String(
									algorithmName + "-" + Constants.MAX_EVALS + "-" + classWithIssues.replace('/', '.')
											+ "." + methodName + ".csv");
							String fileNameForGraph = new String(algorithmName + "-" + Constants.MAX_EVALS + "-"
									+ classWithIssues.replace('/', '.') + "." + methodName + ".dot");
							String fileNameForGraphWithoutConflicts = new String(
									algorithmName + "-" + Constants.MAX_EVALS + "-" + classWithIssues.replace('/', '.')
											+ "." + methodName + ".no-conflicts.dot");
							String fileNameForConflictGraph = new String(algorithmName + "-" + Constants.MAX_EVALS + "-"
									+ classWithIssues.replace('/', '.') + "." + methodName + ".conflicts.dot");

							// Compute and annotate accumulated complexity in AST nodes
							int methodComplexity = Utils
									.computeAndAnnotateAccumulativeCognitiveComplexity((MethodDeclaration) ast);

							// Report the cognitive complexity of the method
							LOGGER.info("CognitiveComplexity (" + methodName + ")=" + methodComplexity);

							// Compute refactoring cache of current method
							LOGGER.info("Computing refactoring cache ...");
							RefactoringCacheFiller.exhaustiveEnumerationAlgorithm(refactoringCache, ast);
							refactoringCache.writeToCSV(Constants.OUTPUT_FOLDER, fileNameForRefactoringCacheInfo);
							LOGGER.info("Refactoring cache for method '" + methodName + "' succesfully generated in '"
									+ fileNameForRefactoringCacheInfo + "'!");

							// root extraction (information about the extraction of the entire body of the
							// method)
							root = new ExtractionVertex(((MethodDeclaration) ast).getStartPosition(),
									((MethodDeclaration) ast).getLength()
											+ ((MethodDeclaration) ast).getStartPosition(),
									methodComplexity,
									(int) ast.getProperty(Constants.ACCUMULATED_INHERENT_COMPLEXITY_COMPONENT),
									(int) ast.getProperty(Constants.ACCUMULATED_NESTING_COMPLEXITY_COMPONENT),
									(int) ast.getProperty(Constants.ACCUMULATED_NUMBER_NESTING_COMPLEXITY_CONTRIBUTORS),
									0);

							Solution solution = new Solution(compilationUnit, ast);
							switch (algorithmName) {
							case Constants.EXHAUSTIVE_SEARCH_LONG_SEQUENCES_FIRST:
								solution = new EnumerativeSearch().run(APPROACH.LONG_SEQUENCE_FIRST, bf,
										classWithIssues, compilationUnit, refactoringCache, auxList, ast,
										methodComplexity);
								break;
							case Constants.EXHAUSTIVE_SEARCH_SHORT_SEQUENCES_FIRST:
								solution = new EnumerativeSearch().run(APPROACH.SHORT_SEQUENCE_FIRST, bf,
										classWithIssues, compilationUnit, refactoringCache, auxList, ast,
										methodComplexity);
								break;
							default:
								LOGGER.severe("No algorithm with name " + algorithmName);
							}

							LOGGER.info(solution.toString());
							if (!solution.getSequenceList().isEmpty())
								solutions.add(neo.reducecognitivecomplexity.sonar.Utils
										.indexOfInsertionToKeepListSorted(solution, solutions), solution);

							refactoringCache.writeToCSV(Constants.OUTPUT_FOLDER, fileNameForRefactoringCacheInfo);

							SimpleGraph<ExtractionVertex, DefaultEdge> conflictsGraph = new SimpleGraph<>(
									DefaultEdge.class);
							SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> graphWithoutConflicts = new SimpleDirectedWeightedGraph<>(
									DefaultWeightedEdge.class);

							// Initialize graphs (the one including conflicts, the one excluding conflicts,
							// and one just including conflicts)
							SimpleDirectedWeightedGraph<ExtractionVertex, DefaultWeightedEdge> graph = refactoringCache
									.getGraphOfFeasibleRefactorings(root, methodComplexity, graphWithoutConflicts,
											conflictsGraph);

							// render full graph
							neo.reducecognitivecomplexity.graphs.Utils.renderGraphInDotFormatInFile(graph,
									Constants.OUTPUT_FOLDER, fileNameForGraph);
							neo.reducecognitivecomplexity.graphs.Utils.clear(graph);

							// render graph without conflicts
							neo.reducecognitivecomplexity.graphs.Utils.renderGraphInDotFormatInFile(
									graphWithoutConflicts, Constants.OUTPUT_FOLDER, fileNameForGraphWithoutConflicts);
							neo.reducecognitivecomplexity.graphs.Utils.clear(graphWithoutConflicts);

							// render conflicts graph
							neo.reducecognitivecomplexity.graphs.Utils.renderConflictGraphInDotFormatInFile(
									conflictsGraph, Constants.OUTPUT_FOLDER, fileNameForConflictGraph);
							neo.reducecognitivecomplexity.graphs.Utils.clear(conflictsGraph);
						}
					}

					LOGGER.info("Refactoring operations to apply in class " + classWithIssues + ":\n" + solutions);
				}

				currentPage++;
				allPagesProccessed = currentPage > totalPagesInSonar;
			} // end while loop to paginating issues in Sonar

			bf.close();
		} catch (SocketTimeoutException e) {
			LOGGER.severe("Wrong communication with SONAR server '" + sonarServer + "'!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
}
