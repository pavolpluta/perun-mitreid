package cz.muni.ics.oidc.models;

import java.util.Objects;

/**
 * Virtual Organization (Vo) object model.
 *
 * @author Dominik Frantisek Bucik <bucik@.ics.muni.cz>
 */
public class Vo extends Model {

	private String name;
	private String shortName;

	public Vo() { }

	public Vo(Long id, String name, String shortName) {
		super(id);
		this.name = name;
		this.shortName = shortName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	@Override
	public String toString() {
		return "Vo{" +
				"id=" + this.getId() +
				", name='" + name + '\'' +
				", shortName='" + shortName + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Vo vo = (Vo) o;
		return Objects.equals(name, vo.name) &&
				Objects.equals(shortName, vo.shortName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name, shortName);
	}
}
