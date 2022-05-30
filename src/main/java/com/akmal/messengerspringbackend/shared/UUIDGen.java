package com.akmal.messengerspringbackend.shared;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 29/05/2022 - 11:17
 * @project messenger-spring-backend
 * @since 1.0
 */
@Slf4j
public class UUIDGen {
  public static void main(String[] args) {
    Path path =
        Paths.get(
            "src"
                + File.separator
                + "main"
                + File.separator
                + "resources"
                + File.separator
                + "ids.txt");

    try {
      Files.deleteIfExists(path);

      Collection<String> ids = new LinkedList<>();
      Collection<String> timeBased = new LinkedList<>();

      ids.add("UUID");
      timeBased.add("\nTIMEUUID");

      for (int i = 0; i < 15; i++) {
        ids.add(Uuids.random().toString());
        timeBased.add(Uuids.timeBased().toString());
      }

      final String content =
          Stream.concat(ids.stream(), timeBased.stream()).collect(Collectors.joining("\n"));

      try (final var writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
        writer.write(content);
      } catch (IOException e) {

      }
    } catch (IOException e) {
      log.error("There was an error while deleting the existing file", e);
    }
  }
}
