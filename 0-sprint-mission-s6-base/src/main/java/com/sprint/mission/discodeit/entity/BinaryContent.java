package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "binary_contents")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
public class BinaryContent extends BaseEntity {


  @Column(nullable = false)
  private String fileName;

  @Column(nullable = false)
  private Long size;

  @Column(length = 100, nullable = false)
  private String contentType;

  @Column(nullable = false)
  private byte[] bytes;

  public BinaryContent(String fileName, Long size, String contentType, byte[] bytes) {
    this.fileName = fileName;
    this.size = size;
    this.contentType = contentType;
    this.bytes = bytes;
  }
}
