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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getVoId() {
		return voId;
	}

	public void setVoId(Long voId) {
		this.voId = voId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
