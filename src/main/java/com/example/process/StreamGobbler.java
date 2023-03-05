package com.example.process;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class StreamGobbler implements Runnable {

  private final InputStream inputStream;
  private final Consumer<String> consumer;

  @Override
  public void run() {
    new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
  }
}
