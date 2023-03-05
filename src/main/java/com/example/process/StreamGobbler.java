package com.example.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

@Slf4j
@RequiredArgsConstructor
public class StreamGobbler implements Callable<String> {

  private final InputStream inputStream;

  @Override
  public String call() {
    log.info("Thread executed");
    var output = new StringBuilder();
    new BufferedReader(new InputStreamReader(inputStream))
        .lines()
        .map(this::appendResult)
        .forEach(output::append);
    return output.toString();
  }

  private String appendResult(String data) {
    return String.format("%s%s", data, System.lineSeparator());
  }
}
