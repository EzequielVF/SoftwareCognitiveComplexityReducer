package neo.reducecognitivecomplexity.algorithms.exhaustivesearch;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ExhaustiveEnumeration<T> {

	private List<Iterable<T>> elementsToIterate;
	private Stack<Iterator<T>> iterators;
	private Stack<T> currentElement;

	private Predicate<Stack<T>> validity;

	public ExhaustiveEnumeration(List<Iterable<T>> elementsToIterate, Predicate<Stack<T>> validity) {
		this.elementsToIterate = elementsToIterate;
		this.validity = validity;

	}

	public void run(Consumer<Stack<T>> consumer, long maxElements) {
		iterators = new Stack<>();
		currentElement = new Stack<>();
		long count = 0;
		if (count >= maxElements) {
			return;
		}

		if (thereAreMoreIteratorsToAdd()) {
			addNewIterator();
		}

		while (!iterators.isEmpty()) {
			if (iterators.peek().hasNext()) {
				iterateOverTopIterator();
				if (validity.test(currentElement)) {
					if (thereAreMoreIteratorsToAdd()) {
						addNewIterator();
					} else {
						consumer.accept(currentElement);
						count++;
						if (count >= maxElements) {
							return;
						}
					}
				}
			} else {
				removeTopIterator();
			}
		}
	}

	public BigInteger count() {
		BigInteger result = BigInteger.ONE;

		for (Iterable<T> iterable : elementsToIterate) {
			long number = 0;
			for (T element : iterable) {
				number++;
			}
			if (number == 0) {
				return BigInteger.ZERO;
			}
			System.out.print(" " + number + ",");
			result = result.multiply(BigInteger.valueOf(number));
		}

		return result;
	}

	protected void removeTopIterator() {
		iterators.pop();
		currentElement.pop();
	}

	protected boolean thereAreMoreIteratorsToAdd() {
		return iterators.size() < elementsToIterate.size();
	}

	protected void iterateOverTopIterator() {
		currentElement.pop();
		currentElement.push(iterators.peek().next());
	}

	protected void addNewIterator() {
		iterators.push(elementsToIterate.get(iterators.size()).iterator());
		currentElement.push(null);
	}
}
