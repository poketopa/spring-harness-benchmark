package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminSetupAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("어드민은 매니저, 매장, 테마, 예약 시간을 생성할 수 있다")
    void adminCreatesManagerStoreThemeAndTime() {
        long managerId = createManager("매니저", "manager@example.com");
        long storeId = createStore("강남점", managerId);
        long themeId = createTheme("어둠의 방", storeId);
        long timeId = createTime(LocalTime.of(10, 0));

        assertThat(managerId).isPositive();
        assertThat(storeId).isPositive();
        assertThat(themeId).isPositive();
        assertThat(timeId).isPositive();
    }
}
