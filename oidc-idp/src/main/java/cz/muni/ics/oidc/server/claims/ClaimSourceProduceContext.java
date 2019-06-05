package cz.muni.ics.oidc.server.claims;

import cz.muni.ics.oidc.models.RichUser;

public class ClaimSourceProduceContext {

	private final long perunUserId;
	private final String sub;
	private RichUser richUser;

	public ClaimSourceProduceContext(long perunUserId, String sub, RichUser richUser) {
		this.perunUserId = perunUserId;
		this.sub = sub;
		this.richUser = richUser;
	}

	public RichUser getRichUser() {
		return richUser;
	}

	public long getPerunUserId() {
		return perunUserId;
	}

	public String getSub() {
		return sub;
	}

	@Override
	public String toString() {
		return "ClaimSourceProduceContext{" +
				"perunUserId=" + perunUserId +
				", sub='" + sub + '\'' +
				//", richUser=" + richUser +
				'}';
	}
}
