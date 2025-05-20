package com.sprint.mission.discodeit.controller;


import com.sprint.mission.discodeit.dto.binarycontent.CreateBinaryContentRequest;
import com.sprint.mission.discodeit.dto.message.CreateMessageRequest;
import com.sprint.mission.discodeit.dto.message.MessageDTO;
import com.sprint.mission.discodeit.dto.message.UpdateMessageRequest;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.service.MessageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

  private final MessageService messageService;

  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<MessageDTO> createMessage(
      @RequestPart("messageCreateRequest") CreateMessageRequest createMessageRequest,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
  ) {
    List<CreateBinaryContentRequest> attachmentRequests = Optional.ofNullable(attachments)
        .map(files -> files.stream()
            .map(file -> {
              try {
                return new CreateBinaryContentRequest(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
                );
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
            .toList())
        .orElse(new ArrayList<>());

    Message message = messageService.create(createMessageRequest, attachmentRequests);
    return ResponseEntity.ok(MessageDTO.fromDomain(message));
  }

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<MessageDTO> find(@RequestParam("id") UUID messageId) {
    Message message = messageService.find(messageId);
    return ResponseEntity.ok(MessageDTO.fromDomain(message));
  }

  @RequestMapping(value = "{channelId}", method = RequestMethod.GET)
  public ResponseEntity<List<MessageDTO>> findAllMessageByChannelId(
      @PathVariable("channelId") UUID channelId) {
    List<Message> messageList = messageService.findAllByChannelId(channelId);
    List<MessageDTO> messageDTO = messageList.stream()
        .map(MessageDTO::fromDomain)
        .toList();
    return ResponseEntity.ok(messageDTO);
  }

  @RequestMapping(value = "{messageId}", method = RequestMethod.PATCH)
  public ResponseEntity<MessageDTO> updateMessage(@PathVariable("messageId") UUID messageId,
      @RequestBody UpdateMessageRequest updateMessageRequest) {
    Message message = messageService.update(messageId, updateMessageRequest);
    return ResponseEntity.ok(MessageDTO.fromDomain(message));
  }

  @RequestMapping(value = "/{messageId}", method = RequestMethod.DELETE)
  public ResponseEntity<String> deleteMessage(@PathVariable("messageId") UUID messageId) {
    messageService.delete(messageId);
    return ResponseEntity.ok("메시지 ID : " + messageId + "삭제 완료");
  }


}
