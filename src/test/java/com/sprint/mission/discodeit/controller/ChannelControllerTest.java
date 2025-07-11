package com.sprint.mission.discodeit.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.GlobalExceptionHandler;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.service.ChannelService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ChannelController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("채널 Controller 슬라이스 테스트")
public class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private AuthController authController;

    @MockitoBean
    private BinaryContentController binaryContentController;

    @MockitoBean
    private MessageController messageController;

    @MockitoBean
    private ReadStatusController readStatusController;

    @MockitoBean
    private UserController userController;

    @Test
    @DisplayName("POST /channels - case : success")
    void createChannelSuccess() throws Exception {
        // Given
        UUID channelId = UUID.randomUUID();
        UserDto user1 = new UserDto(UUID.randomUUID(),"김현기","test1@test.com",null, true);
        UserDto user2 = new UserDto(UUID.randomUUID(),"testUser","tset2@test.com",null, true);
        when(channelService.create(any(PrivateChannelCreateRequest.class)))
            .thenReturn(
                new ChannelDto(
                    channelId,
                    ChannelType.PRIVATE,
                    null,
                    null,
                    List.of(user1,user2),
                    Instant.now())
            );

        // When
        ResultActions result = mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("""
            {
              "participantIds" : ["%s", "%s"]
            }""", user1.id(), user2.id()))
        );

        // Then
        result.andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value(ChannelType.PRIVATE.name()));
    }

    @Test
    @DisplayName("POST /channels - case : 존재하지 않는 유저로 인한 failed")
    void createPrivateChannelFail() throws Exception {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(channelService.create(any(PrivateChannelCreateRequest.class)))
            .thenThrow(new UserNotFoundException());

        // When
        ResultActions result = mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("""
            {
              "participantIds" : ["%s", "%s"]
            }""", id1, id2))
        );

        // Then
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("GET /channels - case : success")
    void findAllChannelsByUserIdSuccess() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID channelId1 = UUID.randomUUID();
        UUID channelId2 = UUID.randomUUID();
        ChannelDto publicChannel = new ChannelDto(
            channelId1,
            ChannelType.PUBLIC,
            "publicChannel",
            "publicChannel description",
            null,
            Instant.now()
        );
        ChannelDto privateChannel = new ChannelDto(
            channelId2,
            ChannelType.PRIVATE,
            null,
            null,
            List.of(),
            Instant.now()
        );
        when(channelService.findAllByUserId(userId)).thenReturn(List.of(publicChannel,privateChannel));

        // When
        ResultActions result = mockMvc.perform(get("/api/channels")
            .param("userId", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("PUBLIC"))
            .andExpect(jsonPath("$[1].type").value("PRIVATE"));
    }

    @DisplayName("GET /channels - case : 유저를 찾을 수 없음으로 인한 failed")
    @Test
    void findAllChannelsByUserIdFail() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        when(channelService.findAllByUserId(userId)).thenThrow(new UserNotFoundException());

        // When
        ResultActions result = mockMvc.perform(get("/api/channels")
            .param("userId", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }
}
