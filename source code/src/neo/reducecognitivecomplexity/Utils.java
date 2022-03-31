package neo.reducecognitivecomplexity;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utilities for search algorithms
 */
public class Utils {
	public static <S, T> Iterable<T> adapt(Iterable<S> it, Function<S, T> fn) {

		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<S> original = it.iterator();

					@Override
					public boolean hasNext() {
						return original.hasNext();
					}

					@Override
					public T next() {
						return fn.apply(original.next());
					}

				};
			}

		};
	}

	public static <K, V> Map<K, V> filterByValue(Map<K, V> map, Predicate<V> predicate) {
		return map.entrySet().stream().filter(entry -> predicate.test(entry.getValue()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
}
