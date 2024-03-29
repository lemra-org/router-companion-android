package org.rm3l.router_companion.utils.tuple;

public class Pair<FIRST, SECOND> {

  public final FIRST first;

  public final SECOND second;

  /**
   * Convenience method for creating an appropriately typed pair.
   *
   * @param a the first object in the Pair
   * @param b the second object in the pair
   * @return a Pair that is templatized with the types of a and b
   */
  public static <FIRST, SECOND> Pair<FIRST, SECOND> create(FIRST a, SECOND b) {
    return new Pair<>(a, b);
  }

  public Pair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Checks the two objects for equality by delegating to their respective {@link
   * Object#equals(Object)} methods.
   *
   * @param o the {@link Pair} to which this one is to be checked for equality
   * @return true if the underlying objects of the Pair are both considered equal
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Pair)) {
      return false;
    }
    Pair<?, ?> p = (Pair<?, ?>) o;
    return objectsEqual(p.first, first) && objectsEqual(p.second, second);
  }

  /**
   * Compute a hash code using the hash codes of the underlying objects
   *
   * @return a hashcode of the Pair
   */
  @Override
  public int hashCode() {
    return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
  }

  private static boolean objectsEqual(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  @Override
  public String toString() {
    return "Pair(first,second)=" + '(' + first + ',' + second + ')';
  }
}
