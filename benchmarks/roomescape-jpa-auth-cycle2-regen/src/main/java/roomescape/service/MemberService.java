package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.dto.ManagerRequest;
import roomescape.dto.MemberRequest;
import roomescape.dto.MemberResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public MemberService(MemberRepository memberRepository, AdminAuthorizationService adminAuthorizationService) {
        this.memberRepository = memberRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        validateDuplicateEmail(request.email());
        Member member = new Member(request.name(), request.email(), request.password());
        return MemberResponse.from(memberRepository.save(member));
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "이미 사용 중인 이메일입니다.");
        }
    }

    @Transactional
    public MemberResponse createManager(LoginMember loginMember, ManagerRequest request) {
        adminAuthorizationService.validateAdmin(loginMember);
        validateDuplicateEmail(request.email());
        Member manager = new Member(request.name(), request.email(), request.password(), Role.MANAGER);
        return MemberResponse.from(memberRepository.save(manager));
    }
}
