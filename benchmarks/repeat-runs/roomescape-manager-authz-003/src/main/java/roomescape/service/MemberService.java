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
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "이미 사용 중인 이메일입니다.");
        }
        Role role = request.role() == null ? Role.USER : request.role();
        Member member = new Member(request.name(), request.email(), request.password(), role);
        return MemberResponse.from(memberRepository.save(member));
    }
}
