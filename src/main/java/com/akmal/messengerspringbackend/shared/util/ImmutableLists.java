package com.akmal.messengerspringbackend.shared.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 03/07/2022 - 20:46
 * @project messenger-spring-backend
 * @since 1.0
 */
public final class ImmutableLists {

  @SafeVarargs
  public static <T> List<T> append(@NotNull List<T> original, @NotNull T... elements) {
    final var mutableList = new LinkedList<>(original);
    mutableList.addAll(Arrays.asList(elements));
    return List.copyOf(mutableList);
  }

  public static <T> List<T> appendAtIndex(@NotNull List<T> original, int index, @NotNull T element) {
    final var mutableList = new ArrayList<>(original);
    mutableList.set(index, element);
    return List.copyOf(mutableList);
  }
}
