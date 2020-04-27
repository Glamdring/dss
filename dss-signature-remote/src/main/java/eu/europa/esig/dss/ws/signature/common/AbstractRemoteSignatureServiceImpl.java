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
package eu.europa.esig.dss.ws.signature.common;

import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsentinel.ApiCallbackAdapter;
import com.logsentinel.ApiException;
import com.logsentinel.LogSentinelClient;
import com.logsentinel.client.model.ActionData;
import com.logsentinel.client.model.ActorData;
import com.logsentinel.client.model.AuditLogEntryType;
import com.logsentinel.client.model.LogResponse;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESTimestampParameters;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESTimestampParameters;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.enumerations.TimestampContainerForm;
import eu.europa.esig.dss.model.BLevelParameters;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.Policy;
import eu.europa.esig.dss.model.SerializableSignatureParameters;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.SignerLocation;
import eu.europa.esig.dss.model.TimestampParameters;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.pades.PAdESTimestampParameters;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.ws.converter.RemoteCertificateConverter;
import eu.europa.esig.dss.ws.converter.RemoteDocumentConverter;
import eu.europa.esig.dss.ws.dto.RemoteCertificate;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteBLevelParameters;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteTimestampParameters;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.XAdESTimestampParameters;

public abstract class AbstractRemoteSignatureServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteSignatureServiceImpl.class);
    
    private LogSentinelClient logSentinelClient;
    private boolean logsentinelIncludeNames;
    
	protected SerializableSignatureParameters getASiCSignatureParameters(ASiCContainerType asicContainerType,
			SignatureForm signatureForm) {
		switch (signatureForm) {
		case CAdES:
			ASiCWithCAdESSignatureParameters asicWithCAdESParameters = new ASiCWithCAdESSignatureParameters();
			asicWithCAdESParameters.aSiC().setContainerType(asicContainerType);
			return asicWithCAdESParameters;
		case XAdES:
			ASiCWithXAdESSignatureParameters asicWithXAdESParameters = new ASiCWithXAdESSignatureParameters();
			asicWithXAdESParameters.aSiC().setContainerType(asicContainerType);
			return asicWithXAdESParameters;
		default:
			throw new DSSException("Unrecognized format (only XAdES or CAdES are allowed with ASiC) : " + signatureForm);
		}
	}

	@SuppressWarnings("unchecked")
	protected SerializableSignatureParameters createParameters(RemoteSignatureParameters remoteParameters) {
		SerializableSignatureParameters parameters = null;
		ASiCContainerType asicContainerType = remoteParameters.getAsicContainerType();
		SignatureForm signatureForm = remoteParameters.getSignatureLevel().getSignatureForm();
		if (asicContainerType != null) {
			parameters = getASiCSignatureParameters(asicContainerType, signatureForm);
		} else {
			switch (signatureForm) {
			case CAdES:
				parameters = new CAdESSignatureParameters();
				break;
			case PAdES:
				PAdESSignatureParameters padesParams = new PAdESSignatureParameters();
				padesParams.setContentSize(9472 * 2); // double reserved space for signature
				parameters = padesParams;
				break;
			case XAdES:
				parameters = new XAdESSignatureParameters();
				break;
			default:
				throw new DSSException("Unsupported signature form : " + signatureForm);
			}
		}

		if (parameters instanceof AbstractSignatureParameters<?>) {
			AbstractSignatureParameters<TimestampParameters> abstractSignatureParameters = (AbstractSignatureParameters<TimestampParameters>) parameters;
			fillParameters(abstractSignatureParameters, remoteParameters);
			return abstractSignatureParameters;
		}

		return parameters;
	}

	protected void fillParameters(AbstractSignatureParameters<TimestampParameters> parameters, RemoteSignatureParameters remoteParameters) {
		parameters.setBLevelParams(toBLevelParameters(remoteParameters.getBLevelParams()));
		parameters.setDetachedContents(RemoteDocumentConverter.toDSSDocuments(remoteParameters.getDetachedContents()));
		parameters.setDigestAlgorithm(remoteParameters.getDigestAlgorithm());
		parameters.setEncryptionAlgorithm(remoteParameters.getEncryptionAlgorithm());
		parameters.setMaskGenerationFunction(remoteParameters.getMaskGenerationFunction());
		parameters.setReferenceDigestAlgorithm(remoteParameters.getReferenceDigestAlgorithm());
		parameters.setSignatureLevel(remoteParameters.getSignatureLevel());
		parameters.setSignaturePackaging(remoteParameters.getSignaturePackaging());
		if (remoteParameters.getContentTimestamps() != null) {
			parameters.setContentTimestamps(TimestampTokenConverter.toTimestampTokens(remoteParameters.getContentTimestamps()));
		}
		parameters.setSignatureTimestampParameters(toTimestampParameters(remoteParameters.getSignatureTimestampParameters(), 
				remoteParameters.getSignatureLevel().getSignatureForm(), remoteParameters.getAsicContainerType()));
		parameters.setArchiveTimestampParameters(toTimestampParameters(remoteParameters.getArchiveTimestampParameters(), 
				remoteParameters.getSignatureLevel().getSignatureForm(), remoteParameters.getAsicContainerType()));
		parameters.setContentTimestampParameters(toTimestampParameters(remoteParameters.getContentTimestampParameters(), 
				remoteParameters.getSignatureLevel().getSignatureForm(), remoteParameters.getAsicContainerType()));
		parameters.setSignWithExpiredCertificate(remoteParameters.isSignWithExpiredCertificate());
		parameters.setGenerateTBSWithoutCertificate(remoteParameters.isGenerateTBSWithoutCertificate());

		RemoteCertificate signingCertificate = remoteParameters.getSigningCertificate();
		if (signingCertificate != null) { // extends do not require signing certificate
			CertificateToken certificateToken = RemoteCertificateConverter.toCertificateToken(signingCertificate);
			parameters.setSigningCertificate(certificateToken);
		}

		List<RemoteCertificate> remoteCertificateChain = remoteParameters.getCertificateChain();
		if (Utils.isCollectionNotEmpty(remoteCertificateChain)) {
			parameters.setCertificateChain(RemoteCertificateConverter.toCertificateTokens(remoteCertificateChain));
		}
		
		if (parameters instanceof PAdESSignatureParameters) {
		    ((PAdESSignatureParameters) parameters).setSignatureImageParameters(remoteParameters.getSignatureImageParameters());
		    ((PAdESSignatureParameters) parameters).setStampImageParameters(remoteParameters.getStampImageParameters());
		}
	}
	
	private BLevelParameters toBLevelParameters(RemoteBLevelParameters remoteBLevelParameters) {
		BLevelParameters bLevelParameters = new BLevelParameters();
		bLevelParameters.setClaimedSignerRoles(remoteBLevelParameters.getClaimedSignerRoles());
		bLevelParameters.setCommitmentTypeIndications(remoteBLevelParameters.getCommitmentTypeIndications());
		bLevelParameters.setSigningDate(remoteBLevelParameters.getSigningDate());
		bLevelParameters.setTrustAnchorBPPolicy(remoteBLevelParameters.isTrustAnchorBPPolicy());
		
		Policy policy = new Policy();
		policy.setDescription(remoteBLevelParameters.getPolicyDescription());
		policy.setDigestAlgorithm(remoteBLevelParameters.getPolicyDigestAlgorithm());
		policy.setDigestValue(remoteBLevelParameters.getPolicyDigestValue());
		policy.setId(remoteBLevelParameters.getPolicyId());
		policy.setQualifier(remoteBLevelParameters.getPolicyQualifier());
		policy.setSpuri(remoteBLevelParameters.getPolicySpuri());
		bLevelParameters.setSignaturePolicy(policy);
		
		SignerLocation signerLocation = new SignerLocation();
		signerLocation.setCountry(remoteBLevelParameters.getSignerLocationCountry());
		signerLocation.setLocality(remoteBLevelParameters.getSignerLocationLocality());
		signerLocation.setPostalAddress(remoteBLevelParameters.getSignerLocationPostalAddress());
		signerLocation.setPostalCode(remoteBLevelParameters.getSignerLocationPostalCode());
		signerLocation.setStateOrProvince(remoteBLevelParameters.getSignerLocationStateOrProvince());
		signerLocation.setStreet(remoteBLevelParameters.getSignerLocationStreet());
		if (!signerLocation.isEmpty()) {
			bLevelParameters.setSignerLocation(signerLocation);
		}
		
		return bLevelParameters;
	}
	
	protected TimestampParameters toTimestampParameters(RemoteTimestampParameters remoteTimestampParameters) {
		TimestampContainerForm timestampForm = remoteTimestampParameters.getTimestampContainerForm();
		if (timestampForm != null) {
			switch (timestampForm) {
				case PDF:
					return toTimestampParameters(remoteTimestampParameters, SignatureForm.PAdES, null);
				case ASiC_E:
					return toTimestampParameters(remoteTimestampParameters, SignatureForm.CAdES, ASiCContainerType.ASiC_E);
				case ASiC_S:
					return toTimestampParameters(remoteTimestampParameters, SignatureForm.CAdES, ASiCContainerType.ASiC_S);
				default:
					throw new DSSException(String.format("Unsupported timestamp container form [%s]", timestampForm.getReadable()));
			}
		} else {
			throw new DSSException("Timestamp container form is not defined!");
		}
	}
	
	protected TimestampParameters toTimestampParameters(RemoteTimestampParameters remoteTimestampParameters, 
			SignatureForm signatureForm, ASiCContainerType asicContainerType) {
		TimestampParameters timestampParameters;
		if (asicContainerType != null) {
			switch (signatureForm) {
				case CAdES:
					ASiCWithCAdESTimestampParameters asicWithCAdESTimestampParameters = new ASiCWithCAdESTimestampParameters(
							remoteTimestampParameters.getDigestAlgorithm());
					asicWithCAdESTimestampParameters.aSiC().setContainerType(asicContainerType);
					timestampParameters = asicWithCAdESTimestampParameters;
					break;
				case XAdES:
					timestampParameters = new XAdESTimestampParameters(remoteTimestampParameters.getDigestAlgorithm(), 
							remoteTimestampParameters.getCanonicalizationMethod());
					break;
				default:
					throw new DSSException(String.format("Unsupported signature form [%s] for asic container type [%s]", signatureForm, asicContainerType));
			}
		} else {
			switch (signatureForm) {
				case CAdES:
					timestampParameters = new CAdESTimestampParameters(remoteTimestampParameters.getDigestAlgorithm());
					break;
				case PAdES:
					timestampParameters = new PAdESTimestampParameters(remoteTimestampParameters.getDigestAlgorithm());
					break;
				case XAdES:
					timestampParameters = new XAdESTimestampParameters(remoteTimestampParameters.getDigestAlgorithm(), 
							remoteTimestampParameters.getCanonicalizationMethod());
					break;
				default:
					throw new DSSException("Unsupported signature form : " + signatureForm);
			}
		}
		return timestampParameters;
	}
	
	protected SignatureValue toSignatureValue(SignatureValueDTO signatureValueDTO) {
		return new SignatureValue(signatureValueDTO.getAlgorithm(), signatureValueDTO.getValue());
	}

    public void setLogSentinelClient(LogSentinelClient logSentinelClient) {
        this.logSentinelClient = logSentinelClient;
    }

    public void setLogsentinelIncludeNames(boolean logsentinelIncludeNames) {
        this.logsentinelIncludeNames = logsentinelIncludeNames;
    }

    /**
     * Send audit trail information to the LogSentinel audit trail service for
     * secure storing
     * 
     * @param document
     * @param params
     */
    protected void logSigningRequest(DSSDocument document, RemoteSignatureParameters params) {
        if (logSentinelClient == null) {
            return;
        }

        CertificateToken loadCertificate = DSSUtils
                .loadCertificate(params.getSigningCertificate().getEncodedCertificate());

        String principal = loadCertificate.getSubjectX500Principal().getName().replace("+", ",");
        LdapName ldapName;
        try {
            ldapName = new LdapName(principal);
        } catch (InvalidNameException ex) {
            throw new DSSException(ex);
        }

        ActorData actor = new ActorData(loadCertificate.getCertificate().getSerialNumber().toString());

        if (logsentinelIncludeNames) {
            String signerNames = ldapName.getRdns().stream().filter(rdn -> rdn.getType().equals("CN"))
                    .map(Rdn::getValue).map(String.class::cast).findFirst().orElse("");
            actor.setActorDisplayName(signerNames);
        }

        ActionData<String> action = new ActionData<>(document.getDigest(params.getDigestAlgorithm()));
        action.setAction("SIGN");
        action.setEntityType("DOCUMENT");
        if (document.getName() != null) {
            action.setEntityId(document.getName().replaceAll(".pdf", ""));
        }
        action.setEntryType(AuditLogEntryType.BUSINESS_LOGIC_ENTRY);

        try {
            logSentinelClient.getAuditLogActions().logAsync(actor, action, new ApiCallbackAdapter<LogResponse>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    LOG.error("Failed to log request", e);
                }
            });
        } catch (ApiException e) {
            LOG.error("Failed to log request", e);
        }
    }
}
