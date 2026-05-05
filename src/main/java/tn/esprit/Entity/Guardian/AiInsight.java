package tn.esprit.Entity.Guardian;

import java.time.LocalDateTime;

public record AiInsight(
	Integer id,
	Integer userId,
	Integer taskId,
	String type,
	String source,
	String payload,
	Integer helpfulVotes,
	Integer unhelpfulVotes,
	LocalDateTime createdAt
) {
}
