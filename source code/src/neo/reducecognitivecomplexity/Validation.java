package neo.reducecognitivecomplexity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.swt.widgets.Display;

import neo.reducecognitivecomplexity.algorithms.Sequence;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;
import neo.reducecognitivecomplexity.jdt.Utils;

public class Validation implements IApplication {
	private static final Logger LOGGER = Logger.getLogger(Validation.class.getName());
	private static final int ARGS = 2;
	private static final String SUFFIX_FOR_OUTPUT_FILE = ".validation";

	/**
	 * The validation has 2 arguments: (1) a file containing a list of solutions to
	 * validate and (2) the name of the Eclipse project in the Eclipse workspace
	 * where solutions (refactorings) will be applied.
	 */
	@Override
	public Object start(IApplicationContext arg) throws Exception {
		Display.getDefault(); // This is required to work in OSX systems: the display must be created in the
								// main thread

		String pathToFileWithSolutions, projectNameInWorkspace, pathForOutputFile;
		String[] args = (String[]) arg.getArguments().get("application.args");

		// Check the number of arguments given
		if (args.length != ARGS) {
			LOGGER.severe("Number of arguments must be " + ARGS + "!");
			return -1;
		}

		// Read app arguments
		pathToFileWithSolutions = args[0];
		projectNameInWorkspace = args[1];

		// Preparing output file
		pathForOutputFile = pathToFileWithSolutions + SUFFIX_FOR_OUTPUT_FILE;
		BufferedWriter bf = new BufferedWriter(new FileWriter(pathForOutputFile, false));

		// Read solutions from file
		List<String> solutionsFromFile = readFile(pathToFileWithSolutions);

		// Iterate over solutions
		for (String s : solutionsFromFile) {
			String[] tokens = s.split(";");
			String className = tokens[1];
			String methodName = tokens[2];
			String solutionStr = tokens[4];
			String pathToFileInWorkspace = projectNameInWorkspace + "/" + className;
			CompilationUnit cu = Utils.createCompilationUnitFromFileInWorkspace(pathToFileInWorkspace);
			List<Sequence> sequenceList = importSolution(cu, solutionStr);

			// Print class under processing
			String msg = "Processing class:       " + className + "\n";
			bf.append(msg);
			System.out.print(msg);

			// Print method under processing
			msg = "Processing method:      " + methodName + "\n";
			bf.append(msg);
			System.out.print(msg);

			// Print sequence list under processing
			msg = "Processing sequence:    " + sequenceList + "\n";
			bf.append(msg);
			System.out.print(msg);

			// Iterate over sequences
			int methodIndex = sequenceList.size();
			boolean error = false;
			while (!sequenceList.isEmpty()) {
				String methodNameForExtraction = methodName + "ForReduction" + methodIndex;
				Sequence sequence = sequenceList.remove(sequenceList.size() - 1);
				CodeExtractionMetrics metrics = sequence.extractSequence(cu, methodNameForExtraction);

				// Print sequence under processing
				msg = "Extraction metrics:     " + metrics + "\n";
				bf.append(msg);
				System.out.print(msg);

				error = !metrics.isApplied();
				if (error) {
					// Print error reason
					msg = "ERROR in extraction:    " + metrics.getReason() + "\n";
					bf.append(msg);
					System.out.print(msg);

					// Print sequence code of problematic sequence
					msg = "This is the sequence:   " + sequence.toString2() + "\n";
					bf.append(msg);
					System.out.print(msg);
				} else {
					cu = Utils.createCompilationUnitFromFileInWorkspace(pathToFileInWorkspace);
				}
				methodIndex--;
			}
			msg = "\n";
			bf.append(msg);
			System.out.print(msg);
		}

		// Close output file
		bf.close();

		return IApplication.EXIT_OK;
	}

	private List<Sequence> importSolution(CompilationUnit cu, String solution) {
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
			sequenceList.add(new Sequence(l));
		}

		return sequenceList;
	}

	private List<String> readFile(String file) {
		List<String> result = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = Files.newBufferedReader(Paths.get(file));
			String line = reader.readLine();
			while (line != null) {
				// read next line
				line = reader.readLine();

				if (line != null)
					result.add(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
}
