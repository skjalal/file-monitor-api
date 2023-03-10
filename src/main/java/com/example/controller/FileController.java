package com.example.controller;

import com.example.model.FileAttribute;
import com.example.process.StreamGobbler;
import lombok.extern.slf4j.Slf4j;
import org.python.util.PythonInterpreter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
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
      var script = new ClassPathResource("/scripts/file-check.py").getInputStream();
      try (var pyInterp = new PythonInterpreter()) {
        pyInterp.set("a", filePath);
        pyInterp.execfile(script);
        var data =
            Optional.ofNullable(pyInterp.get("output"))
                .map(Object::toString)
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
            result
                .substring(result.indexOf("uid", result.indexOf("uid") + 1), result.indexOf("gid"))
                .replace("uid=", ""));
        var stats = Optional.ofNullable(execute("stat %s".formatted(filePath))).orElseThrow();
        Stream.of(stats.split(System.lineSeparator()))
            .filter(d -> d.contains("Access"))
            .findFirst()
            .map(this::extract)
            .ifPresent(fileAttribute::setCreatedBy);
      }

    } catch (Exception e) {
      log.error("Failed to process file", e);
    }
    return fileAttribute;
  }

  private String extract(String d) {
    var dd = d.substring(d.indexOf("Uid:"), d.indexOf("Gid:"));
    return dd.substring(dd.indexOf("/") + 1).replace(")", "").trim();
  }

  private String execute(String command) {
    try {
      log.info("Executing command: {}", command);
      var builder = new ProcessBuilder("bash", "-c", command);
      var process = builder.start();
      log.info("Prepare process object");
      var streamGobbler = new StreamGobbler(process.getInputStream());
      var future = Executors.newSingleThreadExecutor().submit(streamGobbler);
      process.waitFor(5L, TimeUnit.SECONDS);
      return future.get(15L, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("Failed to execute Linux command", e);
      Thread.currentThread().interrupt();
    }
    return null;
  }
}
