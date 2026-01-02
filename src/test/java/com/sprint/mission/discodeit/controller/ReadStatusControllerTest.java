package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.ReadStatusDto;
import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.service.ReadStatusService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReadStatusController WebMvc 테스트")
public class ReadStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReadStatusService readStatusService;

    @Test
    @DisplayName("읽음 정보 생성 시 201 상태와 DTO를 반환한다")
    void create_shouldReturnCreatedReadStatus() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ReadStatusCreateRequest request = new ReadStatusCreateRequest(userId, channelId, Instant.now());
        ReadStatusDto dto = new ReadStatusDto(UUID.randomUUID(), userId, channelId, Instant.now(), true);
        given(readStatusService.create(any(ReadStatusCreateRequest.class))).willReturn(dto);

        // when
        String response = mockMvc.perform(post("/api/readStatuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // then
        assertThat(response).contains(dto.id().toString());
        then(readStatusService).should().create(any(ReadStatusCreateRequest.class));
    }

    @Test
    @DisplayName("읽음 정보 수정 시 200 상태와 갱신 데이터를 반환한다")
    void update_shouldReturnUpdatedReadStatus() throws Exception {
        // given
        UUID readStatusId = UUID.randomUUID();
        ReadStatusUpdateRequest request = new ReadStatusUpdateRequest(Instant.now(), true);
        ReadStatusDto dto = new ReadStatusDto(readStatusId, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), true);
        given(readStatusService.update(any(UUID.class), any(ReadStatusUpdateRequest.class))).willReturn(dto);

        // when
        String response = mockMvc.perform(patch("/api/readStatuses/{id}", readStatusId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // then
        assertThat(response).contains(readStatusId.toString());
        then(readStatusService).should().update(any(UUID.class), any(ReadStatusUpdateRequest.class));
    }

    @Test
    @DisplayName("사용자별 읽음 정보 목록을 조회한다")
    void findAllByUserId_shouldReturnReadStatuses() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        List<ReadStatusDto> dtos = List.of(
            new ReadStatusDto(UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), true)
        );
        given(readStatusService.findAllByUserId(userId)).willReturn(dtos);

        // when
        String response = mockMvc.perform(get("/api/readStatuses").param("userId", userId.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // then
        assertThat(response).contains(userId.toString());
        then(readStatusService).should().findAllByUserId(userId);
    }
}
