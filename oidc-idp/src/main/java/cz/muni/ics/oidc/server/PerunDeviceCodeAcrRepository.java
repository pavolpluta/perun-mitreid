package cz.muni.ics.oidc.server;

import org.mitre.openid.connect.models.Acr;
import org.mitre.openid.connect.models.DeviceCodeAcr;
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
public class PerunDeviceCodeAcrRepository {

	@PersistenceContext(unitName = "defaultPersistenceUnit")
	private EntityManager manager;

	public DeviceCodeAcr getActiveByDeviceCode(String deviceCode) {
		TypedQuery<DeviceCodeAcr> query = manager.createNamedQuery(DeviceCodeAcr.GET_ACTIVE_BY_DEVICE_CODE,
				DeviceCodeAcr.class);
		query.setParameter(DeviceCodeAcr.PARAM_DEVICE_CODE, deviceCode);
		query.setParameter(Acr.PARAM_EXPIRES_AT, now());
		return query.getSingleResult();
	}

	public DeviceCodeAcr getByUserCode(String userCode) {
		TypedQuery<DeviceCodeAcr> query = manager.createNamedQuery(DeviceCodeAcr.GET_BY_USER_CODE, DeviceCodeAcr.class);
		query.setParameter(DeviceCodeAcr.PARAM_USER_CODE, userCode);

		return query.getSingleResult();
	}

	public DeviceCodeAcr getById(Long id) {
		TypedQuery<DeviceCodeAcr> query = manager.createNamedQuery(DeviceCodeAcr.GET_BY_ID, DeviceCodeAcr.class);
		query.setParameter(DeviceCodeAcr.PARAM_ID, id);
		query.setParameter(DeviceCodeAcr.PARAM_EXPIRES_AT, now());

		return query.getSingleResult();
	}

	@Transactional
	public DeviceCodeAcr store(DeviceCodeAcr acr) {
		DeviceCodeAcr tmp = manager.merge(acr);
		manager.flush();
		return tmp;
	}

	@Transactional
	public void remove(Long id) {
		DeviceCodeAcr acr = getById(id);
		if (acr != null) {
			manager.remove(acr);
		}
	}

	@Transactional
	public void deleteExpired() {
		Query query = manager.createNamedQuery(DeviceCodeAcr.DELETE_EXPIRED);
		query.setParameter(DeviceCodeAcr.PARAM_EXPIRES_AT, now());
		query.executeUpdate();
	}

	private long now() {
		return Instant.now().toEpochMilli();
	}

}
