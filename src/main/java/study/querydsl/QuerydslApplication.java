package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManager;

@SpringBootApplication
public class QuerydslApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuerydslApplication.class, args);
	}

	// 스프링 빈으로 등록해서 repository에서 바로 injection 받을 수도 있음 -> but, 외부에서 주입받는 형식으로 쓰게 되면 Test 코드 작성 시 귀찮아질 수도...
//	@Bean
//	JPAQueryFactory jpaQueryFactory(EntityManager em) {
//		return new JPAQueryFactory(em);
//	}

}
