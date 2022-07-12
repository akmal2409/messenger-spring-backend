package com.akmal.messengerspringbackend.dto.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import lombok.With;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.util.Streamable;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 04/06/2022 - 15:34
 * @project messenger-spring-backend
 * @since 1.0
 */
@With
public record ScrollContent<T> (
    String pagingState, // stores the offset from where to start to read
    List<T> content
) implements Streamable<T> {

  public static <T> ScrollContent<T> of(@NotNull List<T> content,
      @Nullable String pagingState) {
    return new ScrollContent<>(pagingState, content);
  }

  @JsonIgnore
  public static <T> ScrollContent<T> empty() {
    return new ScrollContent<>(null, List.of());
  }

  @Contract(value = " -> new", pure = true)
  @Override
  public @NotNull java.util.Iterator<T> iterator() {
    return new Iterator();
  }

  private class Iterator implements java.util.Iterator<T> {
    private int currentIndex;

    private Iterator() {
      this.currentIndex = 0;
    }

    @Override
    public boolean hasNext() {
      return currentIndex < content.size() - 1;
    }

    @Override
    public T next() {
      if (!this.hasNext()) {
        throw new NoSuchElementException(String.format("No element at index %d for size %d",
            this.currentIndex, content.size()));
      }

      return content.get(this.currentIndex++);
    }


    @Override
    public void forEachRemaining(Consumer<? super T> action) {
      for (int i = this.currentIndex; i < content.size(); i++) {
        action.accept(content.get(i));
      }
    }
  }
}
