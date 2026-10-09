package api;

import domain.MemberStatement;
import service.TopEarnersReport;
import util.Fmt;

import java.time.YearMonth;
import java.util.List;

public final class ReportFormatter {
    private ReportFormatter() {}

    public static String statement(MemberStatement s) {
        return String.format("%s님 (%s, %s)%n  포인트 잔액 %s · 이번 달 적립 %s · 적립 %d건%n",
                s.name(), Fmt.mask(s.memberNo()), s.grade(), Fmt.points(s.balance()),
                Fmt.points(s.earnedThisMonth()), s.entries());
    }

    public static String topEarners(YearMonth month, List<TopEarnersReport.Row> rows) {
        StringBuilder sb = new StringBuilder(month + " 적립 상위\n");
        int rank = 1;
        for (TopEarnersReport.Row r : rows) {
            sb.append(String.format("%2d. %-8s %s %s%n", rank++, Fmt.mask(r.memberNo()), r.name(), Fmt.points(r.points())));
        }
        return sb.toString();
    }
}
