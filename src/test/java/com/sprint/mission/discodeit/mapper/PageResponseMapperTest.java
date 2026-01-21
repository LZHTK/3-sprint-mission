package com.sprint.mission.discodeit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.dto.response.PageResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

@DisplayName("PageResponseMapper 단위 테스트")
public class PageResponseMapperTest {

    private final PageResponseMapper mapper = new PageResponseMapper() {};

    @Test
    @DisplayName("fromSlice는 페이징 메타데이터를 매핑한다")
    void fromSlice_mapsMetadata() {
        // given
        SliceImpl<String> slice = new SliceImpl<>(List.of("a", "b"), PageRequest.of(0, 2), true);

        // when
        PageResponse<String> response = mapper.fromSlice(slice, "cursor");

        // then
        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("cursor");
    }

    @Test
    @DisplayName("fromPage는 totalElements를 매핑한다")
    void fromPage_mapsTotal() {
        // given
        PageImpl<String> page = new PageImpl<>(List.of("x"), PageRequest.of(0, 1), 10);

        // when
        PageResponse<String> response = mapper.fromPage(page, "next");

        // then
        assertThat(response.totalElements()).isEqualTo(10);
        assertThat(response.size()).isEqualTo(1);
    }
}
