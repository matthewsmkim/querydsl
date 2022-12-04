package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

// 값을 조회하는 용도로 씀
@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection        // 어노테이션 넣은 후, 'compileQuerydsl' 실행하면, DTO가 Q파일로 생성됨
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

    // repo에서 Dto를 조회한 다음에, dto를 서비스, 컨트롤러 등에서 사용하며 여러 레이어에 걸쳐서 돌아다니는데, 계속 QueryProjection이 흘러다니면서 querydsl에 의존적으로 다니게 됨


}
