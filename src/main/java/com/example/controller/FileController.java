package com.example.controller;

import com.example.model.FileAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@RestController
public class FileController {

  @GetMapping("/")
  public List<FileAttribute> load(@RequestParam String filePath) {
    return Stream.of(filePath.split(";")).map(this::buildAttr).toList();
  }

  @GetMapping("/exec")
  public String exec() {
    try {
      var path = ResourceUtils.getFile("classpath:scripts/file-script.sh").toPath().toString();
      log.info("Shell script path: {}", path);
      String[] cmd = { "bash", "-c", path + " /var/local/test.txt /var/local/output1.txt" };
      log.info("Executing shell script");
      var process = Runtime.getRuntime().exec(cmd);
      if (process.waitFor() == 0) {
        log.info("Executed");
        Files.readAllLines(Path.of("/var/local/output1.txt")).forEach(log::info);
      } else {
        log.error("Failed");
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
    }
    return "GOOD";
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
        reader.lines().map(this::appendResult).forEach(output::append);
      } catch (Exception e) {
        log.error("Failed to execute data stream", e);
      }

      if (process.waitFor() == 0) {
        log.info("Finished");
        result = output.toString();
      } else {
        log.debug("Error...");
        result = "";
      }
      log.info("Result: {}", result);
      process.destroy();
      return result;
    } catch (Exception e) {
      log.error("Failed to execute Linux command", e);
      Thread.currentThread().interrupt();
    }
    return null;
  }

  private void fetchErrorResult(BufferedReader reader) {
    try {
      log.info("Getting Error Reader object: {}", reader.ready());
      reader.lines().map(this::appendResult).forEach(log::info);
    } catch (Exception e) {
      log.error("Failed to fetch error data", e);
    }
  }

  private String appendResult(String data) {
    return String.format("%s%s", data, System.lineSeparator());
  }
}
