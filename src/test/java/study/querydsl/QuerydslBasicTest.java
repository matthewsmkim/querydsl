package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;


    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        String qlString =
                "select m from Member m " + "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void startQuerydsl() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))        //파라미터 바인딩 처리
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
         Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)                  // 굳이 and를 쓰지 않고도 가능
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();               // 멤버의 목록을 리스트로 조회
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();            // 단건 조회. 결과가 없으면 null, 결과가 둘 이상이면 NonUniqueResultException

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();      // SQL에서 리미트 거는 것(limit(1).fetchOne())과 똑같음

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();        // fetchResults() is deprecated

        results.getTotal();
        List<Member> content = results.getResults();


        long total = queryFactory
                .selectFrom(member)
                .fetchCount();         // count 쿼리로 변경해서 count 수 조회

    }


    // fetchCount() 대체 method
    @Test
    public void count() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Long totalCount = queryFactory
                //.select(Wildcard.count) //select count(*)
                .select(member.count()) //select count(member.id)
                .from(member)
                .fetchOne();
        System.out.println("totalCount = " + totalCount);

    }


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(null is last)
     */

    @Test
    public void sort(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


    @Test
    public void paging1() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)          // 몇 번째부터 끊어서 몇 개를 스킵할 거다 ex) offset(1) -> 1개 스킵
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);

    }

    @Test
    public void paging2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)          // 몇 번째부터 끊어서 몇 개를 스킵할 거다 ex) offset(1) -> 1개 스킵
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults()).size().isEqualTo(2);

    }

    @Test
    public void aggregation() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라
     */

    @Test
    public void group() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)     // team의 이름으로 그룹핑
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);


    }

    /**
     * 팀 A에 소속된 모든 회원
     */


    @Test
    public void join() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)                // 일반적인 join
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");


    }

    /**
     * Theta Join
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */

    @Test
    public void theta_join() {                             // 연관관계 없이도 join이 가능함
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)                     // 연관관계가 안 보임
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");


    }


    /**
     *  예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     *  JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t ON t.name = 'teamA';
     */
    @Test
    public void join_on_filterling() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member, team)           // select가 여러가지이므로 Tuple
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */

    @Test
    public void join_on_no_relation() {                             // 연관관계 없이도 join이 가능함
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }


    }

    @PersistenceUnit
    EntityManagerFactory emf;
    @Test
    public void fetchJoinNo() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 로딩이 된 엔티티인지 초기화가 안된 엔티티인지 가르쳐주는 역할
        assertThat(loaded).as("fetch join 미적용").isFalse();


    }

    @Test
    public void fetchJoinUse() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()       // member를 조회할 때 연관 된 team을 한 쿼리로 끌고 옴
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 로딩이 된 엔티티인지 초기화가 안된 엔티티인지 가르쳐주는 역할
        assertThat(loaded).as("fetch join 미적용").isTrue();


    }

    /**
     * 나이가 가장 많은 회원 조회
     */

    @Test
    public void subQuery(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)

                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원 조회
     */

    @Test
    public void subQueryGoe(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)

                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);

    }

    /**
     * 나이가 평균 이상인 회원 조회
     */

    @Test
    public void subQueryIn(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))

                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);

    }


    @Test
    public void simpleProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void tupleProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }

    @Test
    public void findDtoBySetter() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))     // bean을 주입하여 setter 방식으로 프로젝션 주입
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }

    @Test
    public void findDtoByField() {                  // Getter, Setter를 무시하고 필드에다가 값을 쫙 꽂아버림
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }

    @Test
    public void findDtoByConstructor() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))      // 주의! 타입을 잘 맞춰야 함
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }


    @Test
    public void findUserDto() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name")  // 넣어야 하는 이름이 다를 땐 as를 써서 표현
                        , member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }


    }

    @Test
    public void findUserDto_Subquery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name"),       // 넣어야 하는 이름이 다를 땐 as를 써서 표현
                                ExpressionUtils.as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }


    }

    @Test
    public void findDtoByQueryProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        // 조건이 null이냐 아니냐에 따라서 달라지는데...
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));  // member.username = usernameCond 조건을 넣어주기
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));  // member.age = ageCond 조건을 넣어주기
        }


        return queryFactory
                .selectFrom(member)
                .where(builder)     // 위에 builder에서 나온 결과를 딱! 하고 넣어주기. 끝!
                .fetch();

    }


}
