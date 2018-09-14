package cz.muni.ics.oidc.models;

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
}
