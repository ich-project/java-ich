package org.ich.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
