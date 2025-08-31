package neo.reducecognitivecomplexity.sonar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.algorithms.Solution;
import neo.reducecognitivecomplexity.sonar.cognitivecomplexity.CognitiveComplexMethod;
import neo.reducecognitivecomplexity.sonar.cognitivecomplexity.Contribution;

public class Utils {
	private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

	/**
	 * Perform a GET HTTP request to the given URI using the provided token
	 * 
	 * @param uri
	 * @param token
	 * @return The HTTP GET response
	 * @throws IOException
	 * @throws SocketTimeoutException
	 */
	public static String GETRequest(String uri, String token) throws IOException, SocketTimeoutException {
		String result = new String();
		String url = uri;
		String encodedToken = Base64.getEncoder().encodeToString(token.getBytes("utf-8"));

		URL urlObj = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
		connection.setConnectTimeout(20000);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", "Basic " + encodedToken);
		connection.setDoOutput(true);

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = inputReader.readLine()) != null) {
				response.append(inputLine);
			}
			inputReader.close();

			result = response.toString();
		}

		return result;
	}

	/**
	 * Annotate in the AST of a method the contribution to complexity reported by
	 * SONAR.
	 * 
	 * @param compilationUnit  The compilation unit under processing.
	 * @param method           Information provided by SONAR for a method.
	 * @param sequenceOfBlocks List with processed nodes.
	 * @return The AST of the method.
	 */
	public static ASTNode getASTForMethodAnnotatingContributionToCognitiveComplexity(CompilationUnit compilationUnit,
			CognitiveComplexMethod method, List<ASTNode> sequenceOfBlocks) {
		ASTNode result = null, node = null;
		List<Contribution> l = method.getContributionToComplexity();

		try {
			// Locate in the AST the MethodDeclaration node
			result = neo.reducecognitivecomplexity.jdt.Utils.findNode(compilationUnit,
					method.getTextRange().getStartLine(), method.getTextRange().getStartOffset()).getParent();

			// Iterate over complexity blocks reported by SONAR
			for (Contribution c : l) {
				// Locate node in AST
				node = neo.reducecognitivecomplexity.jdt.Utils.findNode(compilationUnit,
						c.getTextRange().getStartLine(), c.getTextRange().getStartOffset());

				// node.getProperty(CONTRIBUTION_TO_COMPLEXITY) != null when this node has
				// already been visited. In this case, the current node refers to a catch
				// clause or else.
				if (node.getProperty(Constants.CONTRIBUTION_TO_COMPLEXITY) == null) {
					sequenceOfBlocks.add(indexOfInsertionToKeepListSorted(node, sequenceOfBlocks), node);
				}

				int previousValue = neo.reducecognitivecomplexity.jdt.Utils.getIntegerPropertyOfNode(node,
						Constants.CONTRIBUTION_TO_COMPLEXITY);
				node.setProperty(Constants.CONTRIBUTION_TO_COMPLEXITY, previousValue + c.getContribution());

				previousValue = neo.reducecognitivecomplexity.jdt.Utils.getIntegerPropertyOfNode(node,
						Constants.CONTRIBUTION_TO_COMPLEXITY_BY_NESTING);
				node.setProperty(Constants.CONTRIBUTION_TO_COMPLEXITY_BY_NESTING, previousValue + c.getNesting());

				String previousText = (String) node.getProperty(Constants.TEXT_RANGE);
				node.setProperty(Constants.TEXT_RANGE, (previousText == null ? "" : previousText) + c.getTextRange());
			}

		} catch (NullPointerException e) {
			LOGGER.severe("There was a fail processing the AST. Are the SONAR and Eclipse projects the same?\n" + compilationUnit.getJavaElement());
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Get the index in a list where a solutions must be inserted to keep the list
	 * sorted Note that solutions are ordered taking into account the offset in
	 * characters of extractions in the compilation unit
	 * 
	 * @param solution to insert in the list
	 * @param list     where insert the solution
	 * @return Position in the list where insert solution to keep the list sorted
	 */
	public static int indexOfInsertionToKeepListSorted(Solution solution, List<Solution> list) {
		int positionOfInsertion = 0;
		boolean nodeInserted = false;
		int startPositionOfNodeToInsert = solution.getSequenceList().get(0).getSiblingNodes().get(0).getStartPosition();

		while (!list.isEmpty() && !nodeInserted && positionOfInsertion < list.size()) {
			int startPositionOfCurrentNode = list.get(positionOfInsertion).getSequenceList().get(0).getSiblingNodes()
					.get(0).getStartPosition();
			if (startPositionOfNodeToInsert < startPositionOfCurrentNode) {
				nodeInserted = true;
			} else
				positionOfInsertion++;
		}

		return positionOfInsertion;
	}

	/**
	 * Get the index in a list where a node must be inserted to keep the list sorted
	 * Note that nodes are ordered taking into account their offset in characters in
	 * the compilation unit
	 * 
	 * @param node to insert in the list
	 * @param list where insert the node
	 * @return Position in the list where insert node to keep the list sorted
	 */
	public static int indexOfInsertionToKeepListSorted(ASTNode node, List<ASTNode> list) {
		int positionOfInsertion = 0;
		boolean nodeInserted = false;
		int startPositionOfNodeToInsert = node.getStartPosition();

		while (!list.isEmpty() && !nodeInserted && positionOfInsertion < list.size()) {
			int startPositionOfCurrentNode = list.get(positionOfInsertion).getStartPosition();
			if (startPositionOfNodeToInsert < startPositionOfCurrentNode) {
				nodeInserted = true;
			} else
				positionOfInsertion++;
		}

		return positionOfInsertion;
	}

	/**
	 * Compose the URI to query in order to get cognitive complexity issues for the
	 * given project, sonar server and page index (results reported by sonar are
	 * usually paginated)
	 * 
	 * @param server
	 * @param project
	 * @param page
	 * @return The URL to query to get cognitive complexity issues of the given
	 *         project
	 */
	public static String composeSonarUri(String server, String project, int page) {
		String result = new String(server + "/api/issues/search?rules=java:S3776" + "&componentKeys=" + project
				+ "&resolved=false" + "&p=" + page + "&pageSize=100");

		return result;
	}
}

