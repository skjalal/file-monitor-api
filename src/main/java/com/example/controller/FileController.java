package com.example.controller;

import com.example.model.FileAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@RestController
public class FileController {

  @GetMapping("/")
  public List<FileAttribute> load(@RequestParam String filePath) {
    return Stream.of(filePath.split(";")).map(this::buildAttr).toList();
  }

  private FileAttribute buildAttr(String filePath) {
    var fileAttribute = new FileAttribute();
    log.info("Search for: {}", filePath);
    try {
      var data =
              Optional.ofNullable(execute(String.format("sudo ausearch -f %s -i", filePath)))
                      .filter(Predicate.not(String::isEmpty))
                      .orElseThrow();
      var result = data.substring(data.lastIndexOf("type=SYSCALL"));
      var path = Path.of(filePath);
      var fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
      fileAttribute.setFileName(filePath);
      fileAttribute.setFileSize(fileAttributes.size());
      var formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
      fileAttribute.setCreatedTime(formatter.format(Date.from(fileAttributes.creationTime().toInstant())));
      fileAttribute.setLastModifiedTime(formatter.format(Date.from(fileAttributes.lastModifiedTime().toInstant())));
      fileAttribute.setLastAccessTime(formatter.format(Date.from(fileAttributes.lastAccessTime().toInstant())));
      fileAttribute.setUpdatedBy(
              result.substring(result.indexOf("uid", result.indexOf("uid") + 1), result.indexOf("gid")).replace("uid=", ""));
      var stats = Optional.ofNullable(execute("stat %s".formatted(filePath))).orElseThrow();
      Stream.of(stats.split(System.lineSeparator()))
              .filter(d -> d.contains("Access"))
              .findFirst().map(d -> {
                var dd = d.substring(d.indexOf("Uid:"), d.indexOf("Gid:"));
                return dd.substring(dd.indexOf("/") + 1).replace(")", "").trim();
              }).ifPresent(fileAttribute::setCreatedBy);
    } catch (Exception e) {
      log.error("Failed to process file", e);
    }
    return fileAttribute;
  }

  private String execute(String command) {
    try {
      log.info("Executing command: {}", command);
      var process = Runtime.getRuntime().exec(command);
      log.info("Prepare process object");
      var output = new StringBuilder();
      String result;
      try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        log.info("Getting Reader object: {}", reader.ready());
        if (process.waitFor(5L, TimeUnit.SECONDS)) {
          reader.lines().map(this::appendResult).forEach(output::append);
          log.info("Finished");
          result = output.toString();
        } else {
          log.debug("Error...");
          result = "";
        }
//        String line;
//        while ((line = reader.readLine()) != null) {
//          output.append(line).append(System.lineSeparator());
//          log.info(line);
//        }

      }
      log.info("Result: {}", result);
      return result;
    } catch (Exception e) {
      log.error("Failed to execute Linux command", e);
      Thread.currentThread().interrupt();
    }
    return null;
  }

  private String appendResult(String data) {
    return String.format("%s%s", data, System.lineSeparator());
  }
}
