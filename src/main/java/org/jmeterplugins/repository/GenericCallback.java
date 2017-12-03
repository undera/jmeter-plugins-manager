package org.jmeterplugins.repository;

public interface GenericCallback<T> {
    void notify(T t);
}