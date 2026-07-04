package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.dto.MemberRequest;
import roomescape.dto.MemberResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        validateUniqueEmail(request.email());
        Member member = new Member(request.name(), request.email(), request.password());
        return MemberResponse.from(memberRepository.save(member));
    }

    private void validateUniqueEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "이미 사용 중인 이메일입니다.");
        }
    }

    @Transactional
    public MemberResponse createManager(MemberRequest request) {
        validateUniqueEmail(request.email());
        Member member = new Member(request.name(), request.email(), request.password(), Role.MANAGER);
        return MemberResponse.from(memberRepository.save(member));
    }
}
