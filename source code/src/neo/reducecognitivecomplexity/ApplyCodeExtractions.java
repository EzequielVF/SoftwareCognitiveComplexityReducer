package neo.reducecognitivecomplexity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.swt.widgets.Display;
import neo.reducecognitivecomplexity.algorithms.Solution;
import neo.reducecognitivecomplexity.jdt.Utils;

/**
 * This is a main procedure of the Eclipse plug-in.
 * <p>
 * The application has 2 arguments: (1) a file containing a list of
 * {@link Solution} ({@link Sequence} of code extractions) to apply and (2) the
 * folder where output files will be written.
 * <p>
 * The output file summary.txt is a ";"-separated file containing the following
 * information: (1) YES if the {@link Solution} ({@link Sequence} of code extractions) was
 * successfully applied, NO otherwise, (2) project name in Eclipse workspace,
 * (3) class name processed, (4) method with cognitive complexity issue
 * processed, (5) {@link Solution} ({@link Sequence} of code extractions) applied.
 */
public class ApplyCodeExtractions implements IApplication {
	private static final Logger LOGGER = Logger.getLogger(ApplyCodeExtractions.class.getName());
	private static final int ARGS = 2;

	@Override
	public Object start(IApplicationContext arg) throws Exception {
		Display.getDefault(); // This is required to work in OSX systems: the display must be created in the

		String pathToFileWithSolutions, outputFolder, pathForOutputFile;
		String[] args = (String[]) arg.getArguments().get("application.args");

		// Check the number of arguments given
		if (args.length != ARGS) {
			LOGGER.severe("Number of arguments must be " + ARGS + "!");
			return -1;
		}

		// Read app arguments
		pathToFileWithSolutions = args[0];
		outputFolder = args[1];
		pathForOutputFile = outputFolder + File.separatorChar + "summary.txt";

		// Read solutions from file
		List<String> solutionsFromFile = readFile(pathToFileWithSolutions, true);
		FileWriter fw = new FileWriter(pathForOutputFile);

		// Iterate over solutions
		for (String s : solutionsFromFile) {
			String[] tokens = s.split(";");

			String projectNameInWorkspace = tokens[0];
			String className = tokens[2];
			String methodName = tokens[3];
			String solutionStr = tokens[5];

			String pathToFileInWorkspace = projectNameInWorkspace + File.separatorChar + className;
			System.out.println("Processing (solution) in file " + pathToFileInWorkspace + " ...");
			CompilationUnit cu = Utils.createCompilationUnitFromFileInWorkspace(pathToFileInWorkspace);

			boolean error = Utils.builtWithCompilationErrors(cu);
			System.out.println("Built with compilation errors? " + error);
			System.out.println(Utils.getCompilationUnitProblems(cu));

			Solution solution = new Solution(cu, solutionStr, 0);

			// Print class under processing
			String msg = "Processing class:       " + className + "\n";
			System.out.print(msg);

			// Print method under processing
			msg = "Processing method:      " + methodName + "\n";
			System.out.print(msg);

			// Print sequence list under processing
			msg = "Processing sequence:    " + solution.toStringForFileFormat() + "\n";
			System.out.print(msg);

			StringJoiner sj = new StringJoiner(String.valueOf(File.separatorChar));
			sj.add(projectNameInWorkspace);
			sj.add(className);
			sj.add(methodName);

			boolean wasApplied = solution.applyExtractMethodsRefactoring(true);
			if (wasApplied) {
				cu = Utils.createCompilationUnitFromFileInWorkspace(pathToFileInWorkspace);
			}

			String applied = (wasApplied) ? "YES" : "NO";
			fw.write(applied);
			fw.write(";");
			fw.write(sj.toString());
			fw.write(";");
			fw.write(solutionStr);
			fw.write("\n");
			fw.flush();
		}

		fw.close();

		return IApplication.EXIT_OK;
	}

	private List<String> readFile(String file, boolean hasHeader) {
		List<String> result = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = Files.newBufferedReader(Paths.get(file));
			String line = reader.readLine();
			if (hasHeader)
				line = reader.readLine();
			while (line != null) {
				result.add(line);

				// read next line
				line = reader.readLine();
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
