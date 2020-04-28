/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.pades;

import java.util.Date;
import java.util.List;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.BLevelParameters;
import eu.europa.esig.dss.model.pades.SignatureImageParameters;
import eu.europa.esig.dss.model.x509.CertificateToken;

/**
 * Defines a list of common PAdES parameters between signature and timestamps
 *
 */
public interface PAdESCommonParameters {
	
	/**
	 * Returns a claimed signing time
	 * @return {@link Date}
	 */
	Date getSigningDate();
	
	/**
	 * Returns a signature/timestampFieldId
	 * @return {@link String} field id
	 */
	String getFieldId();
	
	/**
	 * Returns Filter value
	 * @return {@link String} filter
	 */
	String getFilter();
	
	/**
	 * Returns SubFilter value
	 * @return {@link String} subFilter
	 */
	String getSubFilter();
	
	/**
	 * Returns {@link SignatureImageParameters} for field's visual representation
	 * @return {@link SignatureImageParameters}
	 */
	SignatureImageParameters getImageParameters();
	
	/**
	 * Returns {@link SignatureImageParameters} for field's visual representation of stamps
	 * @return {@link SignatureImageParameters}
	 */
	List<SignatureImageParameters> getStampImageParameters();
	
	/**
	 * Returns a length of the reserved /Contents attribute
	 * @return int content size
	 */
	int getContentSize();
	
	/**
	 * Returns a DigestAlgorithm to be used to hash the signed/timestamped data
	 * @return {@link DigestAlgorithm}
	 */
	DigestAlgorithm getDigestAlgorithm();
	
	BLevelParameters bLevel();
	
	CertificateToken getSigningCertificate();
}
