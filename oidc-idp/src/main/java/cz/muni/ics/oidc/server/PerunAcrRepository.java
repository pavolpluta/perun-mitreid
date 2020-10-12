package cz.muni.ics.oidc.server;

import org.mitre.openid.connect.models.Acr;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.Instant;

/**
 * Repository class for ACR model.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
@Repository
@Transactional(value = "defaultTransactionManager")
public class PerunAcrRepository {

	@PersistenceContext(unitName = "defaultPersistenceUnit")
	private EntityManager manager;

	public Acr getActive(String sub, String clientId, String acr, String state) {
		TypedQuery<Acr> query = manager.createNamedQuery(Acr.GET_ACTIVE, Acr.class);
		query.setParameter(Acr.PARAM_SUB, sub);
		query.setParameter(Acr.PARAM_CLIENT_ID, clientId);
		query.setParameter(Acr.PARAM_ACR, acr);
		query.setParameter(Acr.PARAM_STATE, state);
		query.setParameter(Acr.PARAM_EXPIRES_AT, now());
		return query.getSingleResult();
	}

	public Acr getById(Long id) {
		TypedQuery<Acr> query = manager.createNamedQuery(Acr.GET_BY_ID, Acr.class);
		query.setParameter(Acr.PARAM_ID, id);
		query.setParameter(Acr.PARAM_EXPIRES_AT, now());

		return query.getSingleResult();
	}

	@Transactional
	public Acr store(Acr acr) {
		Acr tmp = manager.merge(acr);
		manager.flush();
		return tmp;
	}

	@Transactional
	public void remove(Long id) {
		Acr acr = getById(id);
		if (acr != null) {
			manager.remove(acr);
		}
	}

	@Transactional
	public void deleteExpired() {
		Query query = manager.createNamedQuery(Acr.DELETE_EXPIRED);
		query.setParameter(Acr.PARAM_EXPIRES_AT, now());
		query.executeUpdate();
	}

	private long now() {
		return Instant.now().toEpochMilli();
	}

}
