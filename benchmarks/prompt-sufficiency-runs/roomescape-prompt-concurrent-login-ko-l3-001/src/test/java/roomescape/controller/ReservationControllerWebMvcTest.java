package roomescape.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.auth.LoginMember;
import roomescape.auth.LoginMemberArgumentResolver;
import roomescape.config.WebConfig;
import roomescape.exception.ErrorCode;
import roomescape.exception.ErrorCodeStatusMapper;
import roomescape.service.AuthService;
import roomescape.service.MyReservationService;
import roomescape.service.ReservationService;

@WebMvcTest(ReservationController.class)
@Import({WebConfig.class, LoginMemberArgumentResolver.class, ErrorCodeStatusMapper.class})
class ReservationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private MyReservationService myReservationService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("예약 생성 요청 값이 비어 있으면 잘못된 요청 응답을 반환한다")
    void invalidReservationCreateRequestReturnsBadRequest() throws Exception {
        when(authService.authenticate("token")).thenReturn(new LoginMember(1L, "브라운"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": null,
                                  "timeId": 1,
                                  "themeId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));
    }
}
