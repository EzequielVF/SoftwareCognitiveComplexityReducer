package neo.reducecognitivecomplexity.jdt;

/**
 * Contain metric stats for a code extraction.
 * 
 * <p>
 * {@code minNumberOfExtractedLinesOfCode} minimum number of extracted lines of
 * code.
 * 
 * <p>
 * {@code maxNumberOfExtractedLinesOfCode} maximum number of extracted lines of
 * code.
 * 
 * <p>
 * {@code meanNumberOfExtractedLinesOfCode} mean number of extracted lines of
 * code.
 * 
 * <p>
 * {@code totalNumberOfExtractedLinesOfCode} total number of extracted lines of
 * code.
 * 
 * <p>
 * {@code minNumberOfParametersInExtractedMethods} minimum number of parameters
 * in extracted methods.
 * 
 * <p>
 * {@code maxNumberOfParametersInExtractedMethods} maximum number of parameters
 * in extracted methods.
 * 
 * <p>
 * {@code meanNumberOfParametersInExtractedMethods} mean number of parameters in
 * extracted methods.
 * 
 * <p>
 * {@code totalNumberOfParametersInExtractedMethods} total number of parameters
 * in extracted methods.
 * 
 * <p>
 * {@code minReductionOfCognitiveComplexity} minimum reduction of cognitive
 * complexity.
 * 
 * <p>
 * {@code maxReductionOfCognitiveComplexity} maximum reduction of cognitive
 * complexity.
 * 
 * <p>
 * {@code meanReductionOfCognitiveComplexity} mean reduction of cognitive
 * complexity.
 * 
 * <p>
 * {@code totalReductionOfCognitiveComplexity} total reduction of cognitive
 * complexity.
 */
public class CodeExtractionMetricsStats {
	/**
	 * Length (in lines of code) of the code extraction.
	 */
	private int minNumberOfExtractedLinesOfCode;
	private int maxNumberOfExtractedLinesOfCode;
	private double meanNumberOfExtractedLinesOfCode;
	private int totalNumberOfExtractedLinesOfCode;

	/**
	 * Number of parameters of the extracted method.
	 */
	private int minNumberOfParametersInExtractedMethods;
	private int maxNumberOfParametersInExtractedMethods;
	private double meanNumberOfParametersInExtractedMethods;
	private int totalNumberOfParametersInExtractedMethods;

	/**
	 * Reduction of cognitive complexity in method after extraction.
	 */
	private int minReductionOfCognitiveComplexity;
	private int maxReductionOfCognitiveComplexity;
	private double meanReductionOfCognitiveComplexity;
	private int totalReductionOfCognitiveComplexity;

	public CodeExtractionMetricsStats(CodeExtractionMetrics[] extractionMetrics) {
		/**
		 * Length (in lines of code) of the code extraction.
		 */
		minNumberOfExtractedLinesOfCode = Integer.MAX_VALUE;
		maxNumberOfExtractedLinesOfCode = Integer.MIN_VALUE;
		meanNumberOfExtractedLinesOfCode = 0.0;
		totalNumberOfExtractedLinesOfCode = 0;

		/**
		 * Number of parameters of the extracted method.
		 */
		minNumberOfParametersInExtractedMethods = Integer.MAX_VALUE;
		maxNumberOfParametersInExtractedMethods = Integer.MIN_VALUE;
		meanNumberOfParametersInExtractedMethods = 0.0;
		totalNumberOfParametersInExtractedMethods = 0;

		/**
		 * Reduction of cognitive complexity in method after extraction.
		 */
		minReductionOfCognitiveComplexity = Integer.MAX_VALUE;
		maxReductionOfCognitiveComplexity = Integer.MIN_VALUE;
		meanReductionOfCognitiveComplexity = 0.0;
		totalReductionOfCognitiveComplexity = 0;

		for (int i = 0; i < extractionMetrics.length; i++) {
			/**
			 * Length (in lines of code) of the code extraction.
			 */
			if (extractionMetrics[i].getNumberOfExtractedLinesOfCode() < minNumberOfExtractedLinesOfCode)
				minNumberOfExtractedLinesOfCode = extractionMetrics[i].getNumberOfExtractedLinesOfCode();

			if (extractionMetrics[i].getNumberOfExtractedLinesOfCode() > maxNumberOfExtractedLinesOfCode)
				maxNumberOfExtractedLinesOfCode = extractionMetrics[i].getNumberOfExtractedLinesOfCode();

			totalNumberOfExtractedLinesOfCode += extractionMetrics[i].getNumberOfExtractedLinesOfCode();

			/**
			 * Number of parameters of the extracted method.
			 */
			if (extractionMetrics[i].getNumberOfParametersInExtractedMethod() < minNumberOfParametersInExtractedMethods)
				minNumberOfParametersInExtractedMethods = extractionMetrics[i].getNumberOfParametersInExtractedMethod();

			if (extractionMetrics[i].getNumberOfParametersInExtractedMethod() > maxNumberOfParametersInExtractedMethods)
				maxNumberOfParametersInExtractedMethods = extractionMetrics[i].getNumberOfParametersInExtractedMethod();

			totalNumberOfParametersInExtractedMethods += extractionMetrics[i].getNumberOfParametersInExtractedMethod();

			/**
			 * Reduction of cognitive complexity in method after extraction.
			 */
			if (extractionMetrics[i].getReductionOfCognitiveComplexity() < minReductionOfCognitiveComplexity)
				minReductionOfCognitiveComplexity = extractionMetrics[i].getReductionOfCognitiveComplexity();

			if (extractionMetrics[i].getReductionOfCognitiveComplexity() > maxReductionOfCognitiveComplexity)
				maxReductionOfCognitiveComplexity = extractionMetrics[i].getReductionOfCognitiveComplexity();

			totalReductionOfCognitiveComplexity += extractionMetrics[i].getReductionOfCognitiveComplexity();
		}

		meanNumberOfExtractedLinesOfCode = (double) totalNumberOfExtractedLinesOfCode / extractionMetrics.length;
		meanNumberOfParametersInExtractedMethods = (double) totalNumberOfParametersInExtractedMethods
				/ extractionMetrics.length;
		meanReductionOfCognitiveComplexity = (double) totalReductionOfCognitiveComplexity / extractionMetrics.length;
	}

	public int getMinNumberOfExtractedLinesOfCode() {
		return minNumberOfExtractedLinesOfCode;
	}

	public int getMaxNumberOfExtractedLinesOfCode() {
		return maxNumberOfExtractedLinesOfCode;
	}

	public int getTotalNumberOfExtractedLinesOfCode() {
		return totalNumberOfExtractedLinesOfCode;
	}

	public double getMeanNumberOfExtractedLinesOfCode() {
		return meanNumberOfExtractedLinesOfCode;
	}

	public int getMinNumberOfParametersInExtractedMethods() {
		return minNumberOfParametersInExtractedMethods;
	}

	public int getMaxNumberOfParametersInExtractedMethods() {
		return maxNumberOfParametersInExtractedMethods;
	}

	public int getTotalNumberOfParametersInExtractedMethods() {
		return totalNumberOfParametersInExtractedMethods;
	}

	public double getMeanNumberOfParametersInExtractedMethods() {
		return meanNumberOfParametersInExtractedMethods;
	}

	public int getMinReductionOfCognitiveComplexity() {
		return minReductionOfCognitiveComplexity;
	}

	public int getMaxReductionOfCognitiveComplexity() {
		return maxReductionOfCognitiveComplexity;
	}

	public int getTotalNumberOfReductionOfCognitiveComplexity() {
		return totalReductionOfCognitiveComplexity;
	}

	public double getMeanReductionOfCognitiveComplexity() {
		return meanReductionOfCognitiveComplexity;
	}
}
