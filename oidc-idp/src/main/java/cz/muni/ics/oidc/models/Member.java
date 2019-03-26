package cz.muni.ics.oidc.models;

import java.util.Objects;

/**
 * Member object model.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class Member extends Model {

	private Long userId;
	private Long voId;
	private String status;

	public Member(Long id, Long userId, Long voId, String status) {
		super(id);
		this.userId = userId;
		this.voId = voId;
		this.status = status;
	}

	public Long getVoId() {
		return voId;
	}

	public String getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "Member{" +
				"id=" + getId() +
				", userId=" + userId +
				", voId=" + voId +
				", status='" + status + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Member member = (Member) o;
		return Objects.equals(userId, member.userId) &&
				Objects.equals(voId, member.voId) &&
				Objects.equals(status, member.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), userId, voId, status);
	}
}
