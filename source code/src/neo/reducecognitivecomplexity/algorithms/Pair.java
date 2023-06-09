package neo.reducecognitivecomplexity.algorithms;

/**
 * A pair contains to integers modeling the starting and ending point (offset in
 * a file) of a code extraction.
 */
public class Pair implements Comparable<Pair> {
	private Integer a;
	private Integer b;

	public Pair(Integer a, Integer b) {
		this.a = a;
		this.b = b;
	}

	public Pair(String pair) {
		this.a = Integer.valueOf(pair.substring(1, pair.indexOf(',')));
		this.b = Integer.valueOf(pair.substring(pair.indexOf(',') + 2, pair.lastIndexOf(']')));
	}

	public Integer getA() {
		return a;
	}

	public Integer getB() {
		return b;
	}
	
	public void setA(Integer a) {
		this.a = a;
	}

	public void setB(Integer b) {
		this.b = b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}

	/**
	 * Compare if the first component of two pairs are the same
	 * 
	 * @param obj1
	 * @param obj2
	 * @return true if the first component of two pairs are equal
	 */
	public static boolean equalsA(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null)
			return false;
		if (obj1.getClass() != obj2.getClass())
			return false;

		if (((Pair) obj1).getA() == null) {
			if (((Pair) obj2).getA() != null)
				return false;
		} else if (!((Pair) obj1).getA().equals(((Pair) obj2).getA()))
			return false;

		return true;
	}

	/**
	 * Compare if the second component of two pairs are the same
	 * 
	 * @param obj1
	 * @param obj2
	 * @return true if the second component of two pairs are equal
	 */
	public static boolean equalsB(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null)
			return false;
		if (obj1.getClass() != obj2.getClass())
			return false;

		if (((Pair) obj1).getB() == null) {
			if (((Pair) obj2).getB() != null)
				return false;
		} else if (!((Pair) obj1).getB().equals(((Pair) obj2).getB()))
			return false;

		return true;
	}

	/**
	 * Compare if p is contained in q
	 * 
	 * @param p
	 * @param q
	 * @return true when p is contained in q, false if not
	 */
	public static boolean isContained(Pair p, Pair q) {
		return (p.getA() >= q.getA() && p.getB() <= q.getB());
	}

	/**
	 * Compare if two pairs are not related (contained) each other
	 * 
	 * @param p
	 * @param q
	 * @return true when two pairs disjoint and false when q is related (contained)
	 *         in p
	 */
	public static boolean disjoint(Pair p, Pair q) {
		return q.getA() > p.getB() || p.getA() > q.getB();
	}

	/**
	 * Compare if two pairs overlap
	 * 
	 * @param p
	 * @param q
	 * @return true is the two pairs overlap, false if not
	 */
	public static boolean overlapping(Pair p, Pair q) {
		return !(disjoint(p, q) || isContained(p, q) || isContained(q, p));
	}

	public String toString() {
		return new String("[" + a + ", " + b + "]");
	}

	@Override
	public int compareTo(Pair q) {
		int result;

		// this is lower than q (return -1)
		if (this.getA() < q.getA())
			result = -1;
		else if (this.getA() > q.getA()) // this is greater than q (return 1)
			result = 1;
		else { // this.getA() and q.getA() are equals
			// we consider this greater than q when q contains this
			if (this.getB() < q.getB())
				result = 1;
			// we consider this lower than q when this contains q
			else if (this.getB() > q.getB())
				result = -1;
			else // both are equals
				result = 0;
		}

		return result;
	}
}
