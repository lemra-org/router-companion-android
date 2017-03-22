package org.rm3l.router_companion.utils.tuple;

/**
 * Created by rm3l on 01/12/15.
 */
public class Tuple3<FIRST, SECOND, THIRD> {

  public final FIRST first;
  public final SECOND second;
  public final THIRD third;

  public Tuple3(FIRST first, SECOND second, THIRD third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  private static boolean objectsEqual(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * Convenience method for creating an appropriately typed Tuple3.
   *
   * @param a the first object in the Tuple3
   * @param b the second object in the Tuple3
   * @return a Tuple3 that is templatized with the types of a and b
   */
  public static <FIRST, SECOND, THIRD> Tuple3<FIRST, SECOND, THIRD> create(FIRST a, SECOND b,
      THIRD c) {
    return new Tuple3<>(a, b, c);
  }

  /**
   * Checks the two objects for equality by delegating to their respective
   * {@link Object#equals(Object)} methods.
   *
   * @param o the {@link Tuple3} to which this one is to be checked for equality
   * @return true if the underlying objects of the Tuple3 are both considered
   * equal
   */
  @Override public boolean equals(Object o) {
    if (!(o instanceof Tuple3)) {
      return false;
    }
    Tuple3<?, ?, ?> p = (Tuple3<?, ?, ?>) o;
    return objectsEqual(p.first, first) && objectsEqual(p.second, second) && objectsEqual(p.third,
        third);
  }

  /**
   * Compute a hash code using the hash codes of the underlying objects
   *
   * @return a hashcode of the Tuple3
   */
  @Override public int hashCode() {
    return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode()) ^ (
        third == null ? 0 : third.hashCode());
  }
}
