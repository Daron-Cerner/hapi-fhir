package ca.uhn.fhir.jpa.model.entity;

/*
 * #%L
 * HAPI FHIR Model
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.util.UrlUtil;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.Date;

@MappedSuperclass
public abstract class BaseResourceIndexedSearchParam extends BaseResourceIndex {
	static final int MAX_SP_NAME = 100;
	/**
	 * Don't change this without careful consideration. You will break existing hashes!
	 */
	private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(0);

	/**
	 * Don't make this public 'cause nobody better be able to modify it!
	 */
	private static final byte[] DELIMITER_BYTES = "|".getBytes(Charsets.UTF_8);
	private static final long serialVersionUID = 1L;

	@GenericField
	@Column(name = "SP_MISSING", nullable = false)
	private boolean myMissing = false;

	@FullTextField
	@Column(name = "SP_NAME", length = MAX_SP_NAME, nullable = false)
	private String myParamName;

	@ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {})
	@JoinColumn(name = "RES_ID", referencedColumnName = "RES_ID", nullable = false)
	//TODO GGG HS: migration guide says just dump these
	//@ContainedIn
	private ResourceTable myResource;

	@Column(name = "RES_ID", insertable = false, updatable = false, nullable = false)
	private Long myResourcePid;

	@FullTextField
	@Column(name = "RES_TYPE", updatable = false, nullable = false, length = Constants.MAX_RESOURCE_NAME_LENGTH)
	private String myResourceType;

	@GenericField
	@Column(name = "SP_UPDATED", nullable = true) // TODO: make this false after HAPI 2.3
	@Temporal(TemporalType.TIMESTAMP)
	private Date myUpdated;

	@Transient
	private transient PartitionSettings myPartitionSettings;

	@Transient
	private transient ModelConfig myModelConfig;

	@Override
	public abstract Long getId();

	public String getParamName() {
		return myParamName;
	}

	public void setParamName(String theName) {
		myParamName = theName;
	}

	public ResourceTable getResource() {
		return myResource;
	}

	public BaseResourceIndexedSearchParam setResource(ResourceTable theResource) {
		myResource = theResource;
		myResourceType = theResource.getResourceType();
		return this;
	}

	@Override
	public <T extends BaseResourceIndex> void copyMutableValuesFrom(T theSource) {
		BaseResourceIndexedSearchParam source = (BaseResourceIndexedSearchParam) theSource;
		myMissing = source.myMissing;
		myParamName = source.myParamName;
		myUpdated = source.myUpdated;
		myModelConfig = source.myModelConfig;
		myPartitionSettings = source.myPartitionSettings;
		setPartitionId(source.getPartitionId());
	}

	public Long getResourcePid() {
		return myResourcePid;
	}

	public String getResourceType() {
		return myResourceType;
	}

	public void setResourceType(String theResourceType) {
		myResourceType = theResourceType;
	}

	public Date getUpdated() {
		return myUpdated;
	}

	public void setUpdated(Date theUpdated) {
		myUpdated = theUpdated;
	}

	public boolean isMissing() {
		return myMissing;
	}

	public BaseResourceIndexedSearchParam setMissing(boolean theMissing) {
		myMissing = theMissing;
		return this;
	}

	public abstract IQueryParameterType toQueryParameterType();

	@Override
	public void setPartitionId(@Nullable RequestPartitionId theRequestPartitionId) {
		super.setPartitionId(theRequestPartitionId);
	}

	public boolean matches(IQueryParameterType theParam) {
		throw new UnsupportedOperationException("No parameter matcher for " + theParam);
	}

	public PartitionSettings getPartitionSettings() {
		return myPartitionSettings;
	}

	public BaseResourceIndexedSearchParam setPartitionSettings(PartitionSettings thePartitionSettings) {
		myPartitionSettings = thePartitionSettings;
		return this;
	}

	public BaseResourceIndexedSearchParam setModelConfig(ModelConfig theModelConfig) {
		myModelConfig = theModelConfig;
		return this;
	}

	public ModelConfig getModelConfig() {
		return myModelConfig;
	}

	public static long calculateHashIdentity(PartitionSettings thePartitionSettings, RequestPartitionId theRequestPartitionId, String theResourceType, String theParamName) {
		return hash(thePartitionSettings, theRequestPartitionId, theResourceType, theParamName);
	}

	/**
	 * Applies a fast and consistent hashing algorithm to a set of strings
	 */
	static long hash(PartitionSettings thePartitionSettings, RequestPartitionId theRequestPartitionId, String... theValues) {
		Hasher hasher = HASH_FUNCTION.newHasher();

		if (thePartitionSettings.isPartitioningEnabled() && thePartitionSettings.isIncludePartitionInSearchHashes() && theRequestPartitionId != null) {
			if (theRequestPartitionId.getPartitionId() != null) {
				hasher.putInt(theRequestPartitionId.getPartitionId());
			}
		}

		for (String next : theValues) {
			if (next == null) {
				hasher.putByte((byte) 0);
			} else {
				next = UrlUtil.escapeUrlParam(next);
				byte[] bytes = next.getBytes(Charsets.UTF_8);
				hasher.putBytes(bytes);
			}
			hasher.putBytes(DELIMITER_BYTES);
		}

		HashCode hashCode = hasher.hash();
		return hashCode.asLong();
	}
}
