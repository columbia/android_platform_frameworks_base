package com.android.server.tmservice;

/**
 * Custom tuple definition
 */
public class Tuple<X, Y, Z> {
  public final X x;
  public final Y y;
  public final Z z;
  public Tuple(X x, Y y, Z z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public boolean equals (Tuple<X, Y, Z> other) {
    return (x == other.x) && (y == other.y) && (z == other.z);
  }
}