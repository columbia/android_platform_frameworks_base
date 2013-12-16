package com.android.server.tmservice;

/**
 * Custom tuple definition to represent values provided to input locations.
 */
public class Tuple<X, Y, T> {
  public final X x;
  public final Y y;
  public final T tag;

  public Tuple(X x, Y y, T tag) {
    this.x = x;
    this.y = y;
    this.tag = tag;
  }

  public boolean equals (Tuple<X, Y, T> other) {
    return (x == other.x) && (y == other.y)
      && (tag == other.tag);
  }

  public String toString() {
    return "x:" + x + " y:" + y + " tag:" + tag;
  }
}