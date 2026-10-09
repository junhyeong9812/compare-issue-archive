package service;

import domain.Member;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Member lookup. Keys are the member number exactly as issued (text, not a number). */
public class MemberDirectory {
    private final Map<String, Member> members = new LinkedHashMap<>();

    public MemberDirectory(List<Member> list) {
        for (Member m : list) {
            String key = key(m.memberNo());
            if (members.putIfAbsent(key, m) != null) {
                throw new IllegalStateException("member number issued twice: " + m.memberNo());
            }
        }
    }

    public Optional<Member> find(String memberNo) {
        return Optional.ofNullable(members.get(key(memberNo)));
    }

    public List<Member> all() {
        return List.copyOf(members.values());
    }

    /** Corporate codes are case-insensitive on the acquirer side ("c7781" == "C7781"). */
    static String key(String memberNo) {
        return memberNo.strip().toUpperCase();
    }
}
