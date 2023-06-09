package neo.reducecognitivecomplexity.refactoringcache;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import neo.reducecognitivecomplexity.algorithms.Sequence;
import neo.reducecognitivecomplexity.refactoringcache.ConsecutiveSequenceIterator.APPROACH;

public class SentenceSequenceSupplier implements Supplier<List<Sequence>> {
	// TODO: potential problem with memory in case of many options

	private SentenceSequenceIterator ssi;
	private Random rnd;
	private List<List<Sequence>> options;

	public SentenceSequenceSupplier(Sequence sentences, RefactoringCache refactoringCache, APPROACH approach,
			long seed) {
		ssi = new SentenceSequenceIterator(sentences, refactoringCache, approach);
		rnd = new Random(seed);
	}

	private void precomputeListOfSequences() {
		if (options == null) {
			options = new ArrayList<>();
			ssi.forEach(l -> options.add(l));
		}
	}

	public List<Sequence> get() {
		precomputeListOfSequences();
		return options.get(rnd.nextInt(options.size()));
	}

	public int numberOfOptions() {
		precomputeListOfSequences();
		return options.size();
	}

	public List<Sequence> getOption(int i) {
		precomputeListOfSequences();
		return options.get(i);
	}

}
