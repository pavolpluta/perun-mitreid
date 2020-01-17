package org.mitre.openid.connect.models;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import static org.mitre.openid.connect.models.Acr.PARAM_ACR;
import static org.mitre.openid.connect.models.Acr.PARAM_EXPIRES_AT;
import static org.mitre.openid.connect.models.Acr.PARAM_SUB;

@Entity
@Table(name = "acrs")
@NamedQueries({
		@NamedQuery(name = Acr.GET_ACTIVE, query = "SELECT acr FROM Acr acr WHERE " +
				"acr.sub = :" + PARAM_SUB +
				" AND acr.clientId = :" + Acr.PARAM_CLIENT_ID +
				" AND acr.acrValues = :" + PARAM_ACR + " AND acr.state = :" + Acr.PARAM_STATE +
				" AND acr.expiresAt >= :" + PARAM_EXPIRES_AT),
		@NamedQuery(name = Acr.GET_BY_ID, query = "SELECT acr from Acr acr WHERE acr.id = :" + Acr.PARAM_ID),
		@NamedQuery(name = Acr.DELETE_EXPIRED, query = "DELETE FROM Acr acr WHERE acr.expiresAt <= :" + Acr.PARAM_EXPIRES_AT)
})
public class Acr {

	public static final String GET_ACTIVE = "Acr.getActive";
	public static final String GET_BY_ID = "Acr.getById";
	public static final String DELETE_EXPIRED = "Acr.deleteExpired";

	public static final String PARAM_ID = "id";
	public static final String PARAM_SUB = "sub";
	public static final String PARAM_CLIENT_ID = "client_id";
	public static final String PARAM_ACR = "acr_values";
	public static final String PARAM_STATE = "state";
	public static final String PARAM_EXPIRES_AT = "expiration";

	private Long id;
	private String sub;
	private String clientId;
	private String acrValues;
	private String state;
	private String shibAuthnContextClass;
	private long expiresAt;

	public Acr() {

	}

	public Acr(String sub, String clientId, String acrValues, String state) {
		this.sub = sub;
		this.clientId = clientId;
		this.acrValues = acrValues;
		this.state = state;
	}

	public Acr(String sub, String clientId, String acrValues, String state, String shibAuthnContextClass) {
		this.sub = sub;
		this.clientId = clientId;
		this.acrValues = acrValues;
		this.state = state;
		this.shibAuthnContextClass = shibAuthnContextClass;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Basic
	@Column(name = "sub")
	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	@Basic
	@Column(name = "client_id")
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@Basic
	@Column(name = "acr_values")
	public String getAcrValues() {
		return acrValues;
	}

	public void setAcrValues(String acrValues) {
		this.acrValues = acrValues;
	}

	@Basic
	@Column(name = "state")
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Basic
	@Column(name = "shib_authn_context_class")
	public String getShibAuthnContextClass() {
		return shibAuthnContextClass;
	}

	public void setShibAuthnContextClass(String shibAuthnContextClass) {
		this.shibAuthnContextClass = shibAuthnContextClass;
	}

	@Basic
	@Column(name = "expiration")
	public long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(long expiresAt) {
		this.expiresAt = expiresAt;
	}

	@Override
	public String toString() {
		return "Acr{" +
				"id=" + id +
				", sub='" + sub + '\'' +
				", clientId='" + clientId + '\'' +
				", acr='" + acrValues + '\'' +
				", state='" + state + '\'' +
				", shibAuthnContextClass='" + shibAuthnContextClass + '\'' +
				", expiration=" + expiresAt +
				'}';
	}
}
