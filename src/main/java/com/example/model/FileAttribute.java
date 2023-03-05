package com.example.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileAttribute {

  private String fileName;
  private Long fileSize;
  private String createdTime;
  private String createdBy;
  private String lastModifiedTime;
  private String updatedBy;
  private String lastAccessTime;
}
