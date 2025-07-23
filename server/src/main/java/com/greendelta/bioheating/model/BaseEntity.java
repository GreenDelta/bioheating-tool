package com.greendelta.bioheating.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.TableGenerator;

@MappedSuperclass
public abstract class BaseEntity {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "entity_seq")
	@TableGenerator(
		name = "entity_seq",
		pkColumnName = "seq_name",
		valueColumnName = "seq_value",
		pkColumnValue = "entity_seq",
		allocationSize = 150,
		table = "tbl_sequences")
	@JsonProperty
	private long id;

	public long id() {
		return id;
	}

	public void id(long id) {
		this.id = id;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(this.getClass().isInstance(obj)))
			return false;
		var other = (BaseEntity) obj;
		if (this.id == 0L && other.id == 0L) {
			return false;
		}
		return this.id == other.id;
	}

	@Override
	public final int hashCode() {
		return id != 0
			? Long.hashCode(id)
			: super.hashCode();
	}

	@Override
	public String toString() {
		return "Entity [type=" + getClass().getSimpleName() + ", id=" + id + "]";
	}
}
