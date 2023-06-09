package neo.reducecognitivecomplexity.algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.ltk.core.refactoring.Change;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetricsStats;
import neo.reducecognitivecomplexity.jdt.Utils;
import neo.reducecognitivecomplexity.jdt.Utils.MethodDeclarationFinderVisitor;
import neo.reducecognitivecomplexity.refactoringcache.RefactoringCache;

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
	 * Initial cognitive complexity of the method
	 */
	private int initialComplexity;
	
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
		this.initialComplexity = Utils.getIntegerPropertyOfNode(method, Constants.ACCUMULATED_COMPLEXITY);
		this.fitness = Double.MAX_VALUE;
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
		this.initialComplexity = Utils.getIntegerPropertyOfNode(method, Constants.ACCUMULATED_COMPLEXITY);
		this.fitness = Double.MAX_VALUE;
	}
	
	/**
	 * Import a Solution for a given {@link CompilationUnit} from a String
	 * 
	 * @param compilationUnit {@link CompilationUnit} to be associated to the
	 *                        Solution
	 * @param solution        representation as a sequence of nodes given by their
	 *                        starting offset in the compilation unit (e.g. [[8520],
	 *                        [10105 10147 10194]])
	 * @param methodCognitiveComplexity Cognitive complexity of the method
	 */
	public Solution(CompilationUnit compilationUnit, String solution, int methodCognitiveComplexity) {
		this.sequenceList = new ArrayList<>();
		this.compilationUnit = compilationUnit;

		// Pattern to match sequences
		Pattern p = Pattern.compile("\\[([0-9\\s]+)\\]");
		Matcher m = p.matcher(solution);

		// Iter over sequences
		while (m.find()) {
			String value = m.group().substring(1, m.group().length() - 1);

			// Get nodes (their start position) in the sequence
			String[] sequence = value.split("([\\s]+)");

			// Create a sequence
			List<ASTNode> l = new ArrayList<ASTNode>();
			for (String nodeStartPosition : sequence) {
				NodeFinder finder = new NodeFinder(compilationUnit, Integer.parseInt(nodeStartPosition), 0);
				ASTNode node = finder.getCoveringNode();
				l.add(node);
			}
			this.sequenceList.add(new Sequence(compilationUnit, l));
		}

		this.method = sequenceList.get(0).getMethodDeclaration();
		this.methodName = ((MethodDeclaration) this.method).getName().toString();
		this.initialComplexity = methodCognitiveComplexity;
		this.fitness = Double.MAX_VALUE;
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
		this.initialComplexity = currentSolution.initialComplexity;
		this.reducedComplexity = currentSolution.reducedComplexity;
		this.extractionMetricsStats = currentSolution.getExtractionMetricsStats();
	}
	
	/**
	 * Compute the fitness of the solution using the provided
	 * {@link RefactoringCache} and returns Solution's metrics
	 * 
	 * @param rf
	 * @return Metrics of the solution
	 */
	public CodeExtractionMetrics evaluate(RefactoringCache rf) {
		int complexityOfNewExtractedMethod;
		CodeExtractionMetrics[] metrics = new CodeExtractionMetrics[sequenceList.size()];
		CodeExtractionMetrics results = new CodeExtractionMetrics(true, "", false, 0, 0, new ArrayList<Change>(),
				new ArrayList<Change>());
		Pair current = null, last = null;
		fitness = sequenceList.size();
		reducedComplexity = 0;
					
		// The list of sequences is processed from right to left
		for (int i = sequenceList.size() - 1; i >= 0; i--) {
			// Evaluate the fitness of the current sequence if not evaluated yet
			if (metrics[i]==null)
				metrics[i] = sequenceList.get(i).evaluate(rf);

			// If the sequence is not feasible, return MAX_FITNESS
			if (!metrics[i].isFeasible()) {
				fitness = Double.MAX_VALUE;
				feasible = false;
				reducedComplexity = 0;
				return metrics[i];
			}

			current = sequenceList.get(i).getOffsetAsPair();
			
			// Another extraction was applied (we update code extraction offsets if needed)
			if (last != null) {				
				// loop for next code extractions in the list
				int indexOfExtractionWhenUpdatingOffsets = i;
				Pair currentInNextExtractions = current;
				
				while (indexOfExtractionWhenUpdatingOffsets >= 0) {
					// Check if the current extraction length (second offset) must be adapted
					if (Pair.isContained(last, currentInNextExtractions)) {
						if (metrics[indexOfExtractionWhenUpdatingOffsets] == null) 
							metrics[indexOfExtractionWhenUpdatingOffsets] = sequenceList.get(indexOfExtractionWhenUpdatingOffsets).evaluate(rf);

						//update reduction of cognitive complexity of sequence containing previous sequence
						metrics[indexOfExtractionWhenUpdatingOffsets].setReductionOfCognitiveComplexity(metrics[indexOfExtractionWhenUpdatingOffsets].getReductionOfCognitiveComplexity()-metrics[i+1].getReductionOfCognitiveComplexity());
						//update accumulated inherent component of sequence containing previous sequence
						metrics[indexOfExtractionWhenUpdatingOffsets].setAccumulatedInherentComponent(metrics[indexOfExtractionWhenUpdatingOffsets].getAccumulatedInherentComponent()-metrics[i+1].getAccumulatedInherentComponent());
						//update number nesting contributors of sequence containing previous sequence
						metrics[indexOfExtractionWhenUpdatingOffsets].setNumberNestingContributors(metrics[indexOfExtractionWhenUpdatingOffsets].getNumberNestingContributors()-metrics[i+1].getNumberNestingContributors());						
						//update accumulated nesting components of sequence containing previous sequence
						metrics[indexOfExtractionWhenUpdatingOffsets].setAccumulatedNestingComponent(metrics[i+1].getNesting()-metrics[indexOfExtractionWhenUpdatingOffsets].getNesting());						
					} 
					
					// moving to next code extraction for next iteration
					if (indexOfExtractionWhenUpdatingOffsets > 0)
						currentInNextExtractions = sequenceList.get(indexOfExtractionWhenUpdatingOffsets - 1).getOffsetAsPair();
					
					indexOfExtractionWhenUpdatingOffsets--;
				}
			}
			
			// The sequence might exceed the complexity
			complexityOfNewExtractedMethod = metrics[i].getCognitiveComplexityOfNewExtractedMethod();
			// We penalize if the sequence exceeds complexity
			if (complexityOfNewExtractedMethod > Constants.MAX_COMPLEXITY) {
				fitness += (complexityOfNewExtractedMethod - Constants.MAX_COMPLEXITY) * 10;
			}
			
			//Accumulate reduced complexity from the initial one so far
			reducedComplexity += metrics[i].getReductionOfCognitiveComplexity();
			
			// Accumulate solution metrics
			results.joinMetrics(metrics[i]);
			
			last = current;
		}

		this.extractionMetricsStats = new CodeExtractionMetricsStats(metrics);
		int finalMethodComplexity = this.initialComplexity - reducedComplexity;

		// We penalize when the main method still have more than MAX_COMPLEXITY
		if (finalMethodComplexity > Constants.MAX_COMPLEXITY) {
			fitness += (finalMethodComplexity - Constants.MAX_COMPLEXITY) * 10;
		}
		
		feasible = results.isFeasible();

		return results;
	}

	/**
	 * Check if the list of {@link Sequence} of nodes can be extracted
	 * 
	 * @return true if the list of {@link Sequence} of nodes can be extracted
	 */
	public boolean isFeasible() {
		return feasible;
	}

	/**
	 * Remove a given {@link Sequence} in the list of sequences
	 * 
	 * @param i {@link Sequence} index to be removed from the list of sequences
	 */
	public void removeSequence(int i) {
		this.sequenceList.remove(i);

	}

	/**
	 * Get the list of sequences
	 * 
	 * @return List of sequences
	 */
	public List<Sequence> getSequenceList() {
		return this.sequenceList;

	}

	/**
	 * Get a given {@link Sequence} from the list of sequences
	 * 
	 * @param i {@link Sequence} index in the list of sequences
	 *
	 * @return the i {@link Sequence} from the list of sequences
	 */
	public Sequence getSequence(int i) {
		return this.sequenceList.get(i);

	}

	/**
	 * Get the number of code extractions (sequences in the list of sequences)
	 * 
	 * @return the number of code extractions
	 */
	public int getSize() {
		return this.sequenceList.size();
	}

	/**
	 * Get name of the method this Solution belongs to
	 * 
	 * @return name of the method this Solution belongs to
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Get method declaration of the method this Solution belongs to
	 * 
	 * @return declaration of the method this Solution belongs to
	 */
	public MethodDeclaration getMethodDeclaration() {
		return (MethodDeclaration) method;
	}

	/**
	 * Check if the Solution contains a given node
	 * 
	 * @param node to check if contained in the Solution
	 * 
	 * @return True if the given node is contained in the Solution
	 */
	public boolean contains(ASTNode node) {
		return sequenceList.stream().anyMatch(s -> s.contains(node));
	}

	@Override
	public String toString() {
		return "Solution [methodName=" + methodName + ", sequenceList=" + sequenceList + ", isFeasible=" + feasible
				+ ", fitness=" + fitness + ", reducedComplexity=" + reducedComplexity + "]";
	}

	/**
	 * Auxiliary toString method providing information about the Solution and the
	 * code extractions that it performs. Useful for debugging
	 * 
	 * @return a String with information of the Solution and the associated code
	 *         extractions
	 */
	public String toStringVerbose() {
		String result = new String();

		result = "Solution [methodName=" + methodName + ", sequenceList=" + sequenceList + ", isFeasible=" + feasible
				+ ", fitness=" + fitness + ", reducedComplexity=" + reducedComplexity + "]\n";

		result = result + "COMPILATION UNIT " + this.compilationUnit.getJavaElement().getPath() + "\n";
		result = result + "Printing sequence list (AST nodes) [" + sequenceList.size() + " code extraction(s)] ...\n";

		int count = 0;
		for (Sequence seq : sequenceList) {
			result = result + "EXTRACTION " + (count + 1) + "\n";
			result = result + "Nodes starting position: " + seq.toString() + "\n";
			result = result + "OFFSET: " + seq.getOffset() + "\n";
			result = result + "CODE: " + seq.toString2() + "\n";
			count++;
		}

		return result;
	}

	/**
	 * Get a string representing the Solution as a list of {@link Sequence}
	 * 
	 * @return a string representing the Solution as a list of {@link Sequence}
	 */
	public String toStringForFileFormat() {
		return this.sequenceList.toString();
	}

	/**
	 * Get the fitness of the Solution
	 * 
	 * @return the quality of the Solution (the greater the value the worst)
	 */
	public double getFitness() {
		return fitness;
	}

	/**
	 * Get the cognitive complexity reduction if the Solution is applied
	 * 
	 * @return the cognitive complexity reduction if the Solution is applied
	 */
	public int getReducedComplexity() {
		return reducedComplexity;
	}

	/**
	 * Get the initial cognitive complexity of the method
	 * 
	 * @return the initial cognitive complexity of the method
	 */
	public int getInitialComplexity() {
		return initialComplexity;
	}
	
	/**
	 * Get metrics of the code extraction associated to this Solution
	 * 
	 * @return metrics of the code extraction associated to this Solution
	 */
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

	/**
	 * Write Solution to file
	 * 
	 * @param absolutePathToFile where Solution will be written
	 */
	public void writeInFile(String absolutePathToFile) {
		try {
			PrintWriter pw;
			pw = new PrintWriter(absolutePathToFile);
			pw.write(this.toStringVerbose());
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Return a list of {@link Pair} containing the sequence of code extractions of
	 * this Solution
	 * 
	 * @return the list of {@link Pair}
	 */
	public List<Pair> get() {
		ArrayList<Pair> result = new ArrayList<>();

		for (Sequence s : this.sequenceList) {
			result.add(s.getOffsetAsPair());
		}

		return result;
	}

	/**
	 * It applies the list of extract method refactoring operations associated to
	 * this solution.
	 * 
	 * TODO Fix the code to work when extractions applied in anonymous classes.
	 * 
	 * @param printDebuggingInformation true to print information during the process
	 * @return true if all extractions were performed
	 * @throws CoreException when processing undoing code extractions
	 */
	public boolean applyExtractMethodsRefactoring(boolean printDebuggingInformation) throws CoreException {
		boolean extractionsApplied = true;
		String methodName = null;
		CodeExtractionMetrics extractionMetrics;
		Pair current = null, last = null;
		List<Pair> pairsForExtractions = get();
		Stack<CodeExtractionMetrics> extractionMetricsStack = new Stack<>();

		int numberImportedLibraries = compilationUnit.imports().size();
		int lenghtPreviousImportedLibraries = Utils.lenghtImportDeclaration(compilationUnit);
		int lenghtImportedLibraries = lenghtPreviousImportedLibraries;
		int deltaInLengthImportedLibraries = 0;
		int deltaInNumberImportedLibraries = 0;

		if (printDebuggingInformation)
			System.out.println("Extracting code " + (this.toStringForFileFormat()) + " ...");

		// loop for code extractions: from back to front
		int indexOfExtraction = pairsForExtractions.size() - 1;
		while (indexOfExtraction >= 0 && extractionsApplied) {
			if (printDebuggingInformation) {
				System.out.println("Applying extraction " + (indexOfExtraction + 1) + " (there are "
						+ pairsForExtractions.size() + ") ...");
				System.out.println(pairsForExtractions.get(indexOfExtraction).toString() + " ...");
			}

			current = pairsForExtractions.get(indexOfExtraction);

			// Another extraction was applied (we update code extraction offsets if needed)
			if (last != null) {
				// This is initially for efficiency. Thus, we just compute this once in the
				// while loop
				ASTNode lastExtractionCall = null;
				int auxiliarLenght = 0;

				// loop for next code extractions in the list
				int indexOfExtractionWhenUpdatingOffsets = indexOfExtraction;
				Pair currentInNextExtractions = current;
				while (indexOfExtractionWhenUpdatingOffsets >= 0) {
					System.out.println("Checking if  " + last + " (last extraction) is contained in "
							+ currentInNextExtractions + " (current extraction) ...");

					// Check if the current extraction length (second offset) must be adapted
					if (Pair.isContained(last, currentInNextExtractions)) {
						// Find call to last code extraction in compilation unit
						if (lastExtractionCall == null) {
							// We search for the call to the last code extraction
							lastExtractionCall = new NodeFinder(compilationUnit, last.getA(), 0).getCoveringNode();
							while (!(lastExtractionCall instanceof SimpleName)
									|| (lastExtractionCall instanceof SimpleName)
											&& !((SimpleName) lastExtractionCall).getIdentifier().equals(methodName)) {
								// When extracting code, file format usually changes (spaces, tabulations,
								// etc.). In order to control this, we use the auxiliarLenght variable to store
								// the number of additional characters added.
								auxiliarLenght++;
								lastExtractionCall = new NodeFinder(compilationUnit, last.getA() + auxiliarLenght, 0)
										.getCoveringNode();
							}
							if (((SimpleName) lastExtractionCall).getIdentifier().equals(methodName))
								lastExtractionCall = lastExtractionCall.getParent();
						}
						// Compute end-offset of the current code extraction:
						// subtracting the length of the last code extraction and adding the length of
						// the call to the extracted method
						int computedEndPosition;
						computedEndPosition = currentInNextExtractions.getB() - (last.getB() - last.getA())
								+ (lastExtractionCall.getLength()) + auxiliarLenght + 1;
						// Adapt offset in case imported libraries have been modified by a previous
						// extraction
						if (deltaInNumberImportedLibraries > 0) {
							computedEndPosition = computedEndPosition - deltaInLengthImportedLibraries
									- deltaInNumberImportedLibraries;
						}

						if (printDebuggingInformation) {
							System.out.println(
									"Extraction " + last + " is contained in " + currentInNextExtractions + " ...");
							System.out.println("Value used to compute lastLOC is " + last.getA());
							System.out.println("LOC where last extraction is now call is "
									+ compilationUnit.getLineNumber(last.getA()));
							System.out.println("Last extraction call was " + lastExtractionCall.toString());
							System.out.println("Initial offset of last call = " + lastExtractionCall.getStartPosition()
									+ ", length of last call = " + (lastExtractionCall.getLength()));
							System.out
									.println("Computed end position for adapted extraction is " + computedEndPosition);
						}
						// update end-offset of the current code extraction
						currentInNextExtractions.setB(computedEndPosition);
					}

					// moving to next code extraction for next iteration
					if (indexOfExtractionWhenUpdatingOffsets > 0)
						currentInNextExtractions = pairsForExtractions.get(indexOfExtractionWhenUpdatingOffsets - 1);

					indexOfExtractionWhenUpdatingOffsets--;
				}
			}

			// Compose method name
			methodName = this.methodName + "_extraction_" + (indexOfExtraction + 1);
			// We force first character to lower case to be accorded to Java convention
			methodName = methodName.replaceFirst("^.", methodName.substring(0, 1).toLowerCase());
			if (printDebuggingInformation)
				System.out.println("New method name = " + methodName);

			// Perform current code extraction
			int length = current.getB() - current.getA();
			if (printDebuggingInformation) {
				System.out.println(current.toString());
				System.out.println("Extracting from " + current.getA() + " length " + length);
			}
			extractionMetrics = Utils.extractCode(compilationUnit, current.getA(), length, methodName, false);
			extractionsApplied = extractionsApplied && extractionMetrics.isApplied();

			// Undo changes (previous extractions) if the current extraction was not applied
			if (!extractionsApplied) {
				System.err.println("ERROR: something wrong when extracting sequence "
						+ sequenceList.get(indexOfExtraction).toString());
				System.err.println(extractionMetrics.getReason());
				System.err.println("Undoing changes ...");
				while (!extractionMetricsStack.isEmpty())
					extractionMetricsStack.pop().getUndoChanges().get(0).perform(null);
			} else {
				extractionMetricsStack.push(extractionMetrics);
				if (printDebuggingInformation) {
					String pathToClass = compilationUnit.getJavaElement().getPath().toOSString();
					System.out.println("Extraction applied succesfully!");
					System.out.println("Reloading compilation unit for class '" + pathToClass + "'");
				}
				compilationUnit = Utils.createCompilationUnitFromFileInWorkspace(
						compilationUnit.getJavaElement().getPath().toOSString());
			}

			last = current;
			indexOfExtraction--;

			// If imports have been modified, offsets must be updated
			if (compilationUnit.imports().size() != numberImportedLibraries) {
				if (printDebuggingInformation) {
					System.out.println("Imports have been modified: updating offsets ...");
				}

				lenghtPreviousImportedLibraries = lenghtImportedLibraries;
				lenghtImportedLibraries = Utils.lenghtImportDeclaration(compilationUnit);

				deltaInLengthImportedLibraries = lenghtImportedLibraries - lenghtPreviousImportedLibraries;
				deltaInNumberImportedLibraries = compilationUnit.imports().size() - numberImportedLibraries;
				for (int index = indexOfExtraction; index >= 0; index--) {
					pairsForExtractions.get(index).setA(pairsForExtractions.get(index).getA()
							+ deltaInLengthImportedLibraries + deltaInNumberImportedLibraries);
					pairsForExtractions.get(index).setB(pairsForExtractions.get(index).getB()
							+ deltaInLengthImportedLibraries + deltaInNumberImportedLibraries);
				}

				numberImportedLibraries = compilationUnit.imports().size();
			} else {
				deltaInLengthImportedLibraries = 0;
				deltaInNumberImportedLibraries = 0;
			}
		}

		return extractionsApplied;
	}

	/**
	 * It applies the list of extract method refactoring operations associated to
	 * this solution. For each extraction, it creates a file containing the
	 * resulting method after such extraction. It also creates a file with the
	 * original source code of the method before any code extraction. This is useful
	 * to know which are the modifications of the method after each extraction.
	 * 
	 * TODO Fix the code to work when extractions applied in anonymous classes.
	 * 
	 * @param printDebuggingInformation true to print information during the process
	 * @param folder                    place where output files will be generated
	 * @return true if all extractions were performed
	 * @throws IOException   when processing files
	 * @throws CoreException when processing undoing code extractions
	 */
	public boolean applyExtractMethodsRefactoringWritingExtractionsInFiles(boolean printDebuggingInformation,
			String folder) throws IOException, CoreException {
		boolean extractionsApplied = true;
		String methodName = null;
		CodeExtractionMetrics extractionMetrics;
		Pair current = null, last = null;
		List<Pair> pairsForExtractions = get();
		Stack<CodeExtractionMetrics> extractionMetricsStack = new Stack<>();

		int numberImportedLibraries = compilationUnit.imports().size();
		int lenghtPreviousImportedLibraries = Utils.lenghtImportDeclaration(compilationUnit);
		int lenghtImportedLibraries = lenghtPreviousImportedLibraries;
		int deltaInLengthImportedLibraries = 0;
		int deltaInNumberImportedLibraries = 0;

		if (printDebuggingInformation)
			System.out.println("Extracting code " + (this.toStringForFileFormat()) + " ...");

		String outputFile = compilationUnit.getJavaElement().getPath().toString();
		outputFile = outputFile.substring(1, outputFile.length() - (".java").length());
		outputFile = outputFile.replace(File.separatorChar, '.');
		outputFile += "." + this.getMethodName() + ".Original.java";
		outputFile = folder + File.separatorChar + outputFile;
		FileWriter fw = new FileWriter(outputFile);
		fw.write(this.getMethodDeclaration().toString());
		fw.close();

		// loop for code extractions: from back to front
		int indexOfExtraction = pairsForExtractions.size() - 1;
		while (indexOfExtraction >= 0 && extractionsApplied) {
			if (printDebuggingInformation) {
				System.out.println("Applying extraction " + (indexOfExtraction + 1) + " (there are "
						+ pairsForExtractions.size() + ") ...");
				System.out.println(pairsForExtractions.get(indexOfExtraction).toString() + " ...");
			}

			current = pairsForExtractions.get(indexOfExtraction);

			// Another extraction was applied (we update code extraction offsets if needed)
			if (last != null) {
				// This is initially for efficiency. Thus, we just compute this once in the
				// while loop
				ASTNode lastExtractionCall = null;
				int auxiliarLenght = 0;

				// loop for next code extractions in the list
				int indexOfExtractionWhenUpdatingOffsets = indexOfExtraction;
				Pair currentInNextExtractions = current;
				while (indexOfExtractionWhenUpdatingOffsets >= 0) {
					System.out.println("Checking if  " + last + " (last extraction) is contained in "
							+ currentInNextExtractions + " (current extraction) ...");

					// Check if the current extraction length (second offset) must be adapted
					if (Pair.isContained(last, currentInNextExtractions)) {
						// Find call to last code extraction in compilation unit
						if (lastExtractionCall == null) {
							// We search for the call to the last code extraction
							lastExtractionCall = new NodeFinder(compilationUnit, last.getA(), 0).getCoveringNode();
							while (!(lastExtractionCall instanceof SimpleName)
									|| (lastExtractionCall instanceof SimpleName)
											&& !((SimpleName) lastExtractionCall).getIdentifier().equals(methodName)) {
								// When extracting code, file format usually changes (spaces, tabulations,
								// etc.). In order to control this, we use the auxiliarLenght variable to store
								// the number of additional characters added.
								auxiliarLenght++;
								lastExtractionCall = new NodeFinder(compilationUnit, last.getA() + auxiliarLenght, 0)
										.getCoveringNode();
							}
							if (((SimpleName) lastExtractionCall).getIdentifier().equals(methodName))
								lastExtractionCall = lastExtractionCall.getParent();
						}
						// Compute end-offset of the current code extraction:
						// subtracting the length of the last code extraction and adding the length of
						// the call to the extracted method
						int computedEndPosition;
						computedEndPosition = currentInNextExtractions.getB() - (last.getB() - last.getA())
								+ (lastExtractionCall.getLength()) + auxiliarLenght + 1;
						// Adapt offset in case imported libraries have been modified by a previous
						// extraction
						if (deltaInNumberImportedLibraries > 0) {
							computedEndPosition = computedEndPosition - deltaInLengthImportedLibraries
									- deltaInNumberImportedLibraries;
						}

						if (printDebuggingInformation) {
							System.out.println(
									"Extraction " + last + " is contained in " + currentInNextExtractions + " ...");
							System.out.println("Value used to compute lastLOC is " + last.getA());
							System.out.println("LOC where last extraction is now call is "
									+ compilationUnit.getLineNumber(last.getA()));
							System.out.println("Last extraction call was " + lastExtractionCall.toString());
							System.out.println("Initial offset of last call = " + lastExtractionCall.getStartPosition()
									+ ", length of last call = " + (lastExtractionCall.getLength()));
							System.out
									.println("Computed end position for adapted extraction is " + computedEndPosition);
						}
						// update end-offset of the current code extraction
						currentInNextExtractions.setB(computedEndPosition);
					}

					// moving to next code extraction for next iteration
					if (indexOfExtractionWhenUpdatingOffsets > 0)
						currentInNextExtractions = pairsForExtractions.get(indexOfExtractionWhenUpdatingOffsets - 1);

					indexOfExtractionWhenUpdatingOffsets--;
				}
			}

			// Compose method name
			methodName = this.methodName + "_extraction_" + (indexOfExtraction + 1);
			// We force first character to lower case to be accorded to Java convention
			methodName = methodName.replaceFirst("^.", methodName.substring(0, 1).toLowerCase());
			if (printDebuggingInformation)
				System.out.println("New method name = " + methodName);

			// Perform current code extraction
			int length = current.getB() - current.getA();
			if (printDebuggingInformation) {
				System.out.println(current.toString());
				System.out.println("Extracting from " + current.getA() + " length " + length);
			}
			extractionMetrics = Utils.extractCode(compilationUnit, current.getA(), length, methodName, false);
			extractionsApplied = extractionsApplied && extractionMetrics.isApplied();

			// Undo changes (previous extractions) if the current extraction was not applied
			if (!extractionsApplied) {
				System.err.println("ERROR: something wrong when extracting sequence "
						+ sequenceList.get(indexOfExtraction).toString());
				System.err.println(extractionMetrics.getReason());
				System.err.println("Undoing changes ...");
				while (!extractionMetricsStack.isEmpty())
					extractionMetricsStack.pop().getUndoChanges().get(0).perform(null);
			} else {
				extractionMetricsStack.push(extractionMetrics);
				if (printDebuggingInformation) {
					String pathToClass = compilationUnit.getJavaElement().getPath().toOSString();
					System.out.println("Extraction applied succesfully!");
					System.out.println("Reloading compilation unit for class '" + pathToClass + "'");
				}
				compilationUnit = Utils.createCompilationUnitFromFileInWorkspace(
						compilationUnit.getJavaElement().getPath().toOSString());
				outputFile = this.compilationUnit.getJavaElement().getPath().toString();
				outputFile = outputFile.substring(1, outputFile.length() - (".java").length());
				outputFile = outputFile.replace(File.separatorChar, '.');
				outputFile += "." + this.methodName + ".Extraction" + (pairsForExtractions.size() - indexOfExtraction)
						+ ".java";
				outputFile = folder + File.separatorChar + outputFile;
				fw = new FileWriter(outputFile);
				MethodDeclarationFinderVisitor methodFinderVisitor = new MethodDeclarationFinderVisitor(
						compilationUnit.getRoot(), this.methodName, this.getMethodDeclaration().parameters());
				fw.write(methodFinderVisitor.getMethodDeclaration().toString());
				fw.close();
			}

			last = current;
			indexOfExtraction--;

			// If imports have been modified, offsets must be updated
			if (compilationUnit.imports().size() != numberImportedLibraries) {
				if (printDebuggingInformation) {
					System.out.println("Imports have been modified: updating offsets ...");
				}

				lenghtPreviousImportedLibraries = lenghtImportedLibraries;
				lenghtImportedLibraries = Utils.lenghtImportDeclaration(compilationUnit);

				deltaInLengthImportedLibraries = lenghtImportedLibraries - lenghtPreviousImportedLibraries;
				deltaInNumberImportedLibraries = compilationUnit.imports().size() - numberImportedLibraries;
				for (int index = indexOfExtraction; index >= 0; index--) {
					pairsForExtractions.get(index).setA(pairsForExtractions.get(index).getA()
							+ deltaInLengthImportedLibraries + deltaInNumberImportedLibraries);
					pairsForExtractions.get(index).setB(pairsForExtractions.get(index).getB()
							+ deltaInLengthImportedLibraries + deltaInNumberImportedLibraries);
				}

				numberImportedLibraries = compilationUnit.imports().size();
			} else {
				deltaInLengthImportedLibraries = 0;
				deltaInNumberImportedLibraries = 0;
			}
		}

		return extractionsApplied;
	}
	
	/**
	* Import a Solution for a given {@link CompilationUnit} from a String
	* 
	* @param compilationUnit {@link CompilationUnit} to be associated to the
	*                        Solution
	* @param solution        representation as a sequence of nodes given by their
	*                        starting offset in the compilation unit (e.g. [[8520],
	*                        [10105 10147 10194]])                        
	*/
	public static List<Sequence> importSolution(CompilationUnit cu, String solution) {
		List<Sequence> sequenceList = new ArrayList<>();

		// Pattern to match sequences
		Pattern p = Pattern.compile("\\[([0-9\\s]+)\\]");
		Matcher m = p.matcher(solution);

		// Iter over sequences
		while (m.find()) {
			String value = m.group().substring(1, m.group().length() - 1);

			// Get nodes (their start position) in the sequence
			String[] sequence = value.split("([\\s]+)");

			// Create a sequence
			List<ASTNode> l = new ArrayList<ASTNode>();
			for (String nodeStartPosition : sequence) {
				NodeFinder finder = new NodeFinder(cu, Integer.parseInt(nodeStartPosition), 0);
				ASTNode node = finder.getCoveringNode();
				l.add(node);
			}
			sequenceList.add(new Sequence(cu, l));
		}

		return sequenceList;
	}
}
